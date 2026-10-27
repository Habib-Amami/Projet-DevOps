FROM jenkins/jenkins:2.580.1-jdk21

USER root

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        ca-certificates \
        curl \
        git \
        gnupg \
        jq \
        lsb-release \
        openssh-client \
        unzip \
    && rm -rf /var/lib/apt/lists/*

# Node.js 24 is required by the Angular frontend.
RUN curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Docker CLI only. Docker Desktop or Docker Engine provides the daemon.
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins
