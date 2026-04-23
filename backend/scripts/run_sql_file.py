"""Run a .sql file against Postgres. Set CSMS_DB_HOST, CSMS_DB_PORT, CSMS_DB_NAME, CSMS_DB_USER, CSMS_DB_PASSWORD."""
import os
import sys

import psycopg2
import sqlparse

def main():
    path = sys.argv[1]
    sql = open(path, encoding="utf-8").read()
    host = os.environ.get("CSMS_DB_HOST", "")
    port = int(os.environ.get("CSMS_DB_PORT", "5432"))
    dbname = os.environ.get("CSMS_DB_NAME", "postgres")
    user = os.environ.get("CSMS_DB_USER", "")
    password = os.environ.get("CSMS_DB_PASSWORD", "")
    if not all([host, user, password]):
        print("Set CSMS_DB_HOST, CSMS_DB_USER, CSMS_DB_PASSWORD", file=sys.stderr)
        sys.exit(1)
    conn = psycopg2.connect(
        host=host,
        port=port,
        dbname=dbname,
        user=user,
        password=password,
        sslmode="require",
    )
    conn.autocommit = False
    cur = conn.cursor()
    try:
        for stmt in sqlparse.split(sql):
            s = stmt.strip()
            if not s:
                continue
            cur.execute(s)
        conn.commit()
        print("Migration applied OK")
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
