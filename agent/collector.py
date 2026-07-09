"""
DB 상태 지표 수집 에이전트.
대상 DB에서 SHOW GLOBAL STATUS를 조회해 Spring API로 push한다.
실무 구조: 에이전트는 DB 서버 로컬에 상주하며 로컬 접속 권한만 가진다.
"""
import time
import logging

import mysql.connector
import requests
import yaml

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("dbops-agent")

STATUS_KEYS = ("Threads_connected", "Slow_queries", "Uptime", "Questions")


def load_config(path: str = "config.yaml") -> dict:
    with open(path, encoding="utf-8") as f:
        return yaml.safe_load(f)


def collect(target: dict) -> dict | None:
    """대상 DB 1대의 지표를 수집. 실패 시 None."""
    try:
        conn = mysql.connector.connect(
            host=target["host"], port=target["port"],
            user=target["user"], password=target["password"],
            connection_timeout=3,
        )
        cursor = conn.cursor()
        cursor.execute(
            "SHOW GLOBAL STATUS WHERE Variable_name IN (%s, %s, %s, %s)",
            STATUS_KEYS,
        )
        status = dict(cursor.fetchall())
        cursor.close()
        conn.close()

        return {
            "threadsConnected": int(status["Threads_connected"]),
            "slowQueries": int(status["Slow_queries"]),
            "uptimeSec": int(status["Uptime"]),
            "questions": int(status["Questions"]),
        }
    except Exception as e:
        log.warning("수집 실패 instance=%s: %s", target["instance_id"], e)
        return None


def push(api_base: str, instance_id: int, metric: dict) -> None:
    url = f"{api_base}/api/instances/{instance_id}/metrics"
    try:
        res = requests.post(url, json=metric, timeout=3)
        res.raise_for_status()
        log.info("push 완료 instance=%s conn=%s slow=%s",
                 instance_id, metric["threadsConnected"], metric["slowQueries"])
    except Exception as e:
        log.warning("push 실패 instance=%s: %s", instance_id, e)


def main() -> None:
    config = load_config()
    log.info("에이전트 시작 - 대상 %d대, 주기 %d초",
             len(config["targets"]), config["interval_sec"])

    while True:
        for target in config["targets"]:
            metric = collect(target)
            if metric is not None:
                push(config["api_base"], target["instance_id"], metric)
        time.sleep(config["interval_sec"])


if __name__ == "__main__":
    main()