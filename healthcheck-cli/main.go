// 전체 인스턴스 병렬 TCP 헬스체크 CLI.
// Spring 스케줄러의 순차 점검(타임아웃 × N)과 달리
// goroutine으로 동시에 점검한다 -> 전체 소요 = 가장 느린 1대의 시간.
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"net"
	"net/http"
	"sync"
	"time"
)

type Instance struct {
	ID   int64  `json:"id"`
	Name string `json:"name"`
	Host string `json:"host"`
	Port int    `json:"port"`
}

type Result struct {
	Instance Instance
	Alive    bool
	Latency  time.Duration
}

// Spring API에서 인스턴스 목록 조회
func fetchInstances(apiBase string) ([]Instance, error) {
	res, err := http.Get(apiBase + "/api/instances")
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	var instances []Instance
	if err := json.NewDecoder(res.Body).Decode(&instances); err != nil {
		return nil, err
	}
	return instances, nil
}

// TCP 접속 검사 (포트 도달 가능성만 판별하므로 자격증명 불필요)
func check(inst Instance, timeout time.Duration) Result {
	addr := fmt.Sprintf("%s:%d", inst.Host, inst.Port)
	start := time.Now()

	conn, err := net.DialTimeout("tcp", addr, timeout)
	latency := time.Since(start)

	if err != nil {
		return Result{Instance: inst, Alive: false, Latency: latency}
	}
	conn.Close()
	return Result{Instance: inst, Alive: true, Latency: latency}
}

func main() {
	apiBase := flag.String("api", "http://localhost:8080", "Spring API 주소")
	timeout := flag.Duration("timeout", 3*time.Second, "접속 타임아웃")
	flag.Parse()

	instances, err := fetchInstances(*apiBase)
	if err != nil {
		fmt.Println("인스턴스 목록 조회 실패:", err)
		return
	}

	fmt.Printf("병렬 헬스체크 시작 - 대상 %d대\n\n", len(instances))
	totalStart := time.Now()

	var wg sync.WaitGroup
	results := make([]Result, len(instances))

	for i, inst := range instances {
		wg.Add(1)
		go func(idx int, in Instance) { // 인스턴스마다 goroutine 1개
			defer wg.Done()
			results[idx] = check(in, *timeout)
		}(i, inst)
	}
	wg.Wait() // 전체 완료 대기

	fmt.Printf("%-4s %-12s %-22s %-12s %s\n", "ID", "NAME", "ADDR", "STATUS", "LATENCY")
	for _, r := range results {
		status := "OK"
		if !r.Alive {
			status = "UNREACHABLE"
		}
		fmt.Printf("%-4d %-12s %-22s %-12s %v\n",
			r.Instance.ID, r.Instance.Name,
			fmt.Sprintf("%s:%d", r.Instance.Host, r.Instance.Port),
			status, r.Latency.Round(time.Millisecond))
	}
	fmt.Printf("\n전체 소요: %v (순차였다면 최대 %v)\n",
		time.Since(totalStart).Round(time.Millisecond),
		(*timeout)*time.Duration(len(instances)))
}