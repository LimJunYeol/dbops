# target-db-1 커스텀 MySQL 설정
# file.managed = "이 파일이 이 내용으로 존재해야 한다"는 상태 선언 (명령이 아님)
mysql_custom_config:
  file.managed:
    - name: /etc/mysql-conf/custom.cnf
    - contents: |
        [mysqld]
        max_connections = 200
        slow_query_log = 1
        long_query_time = 2