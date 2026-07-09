FROM python:3.10-slim

RUN apt-get update && apt-get install -y --no-install-recommends gcc \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir \
    salt==3006.9 \
    pycryptodomex \
    cryptography \
    pyzmq \
    msgpack \
    jinja2 \
    pyyaml \
    distro \
    looseversion \
    packaging

RUN mkdir -p /etc/salt /srv/salt /var/cache/salt /var/log/salt /var/run/salt