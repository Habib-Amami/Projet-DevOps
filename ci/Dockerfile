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

# Docker CLI and Buildx.
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins/docker-buildx /usr/local/libexec/docker/cli-plugins/docker-buildx

RUN docker buildx version

# Terraform CLI.
COPY --from=hashicorp/terraform:1.14.3 /bin/terraform /usr/local/bin/terraform
RUN terraform version

# AWS CLI v2.
RUN curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip \
    && unzip -q /tmp/awscliv2.zip -d /tmp \
    && /tmp/aws/install \
    && rm -rf /tmp/aws /tmp/awscliv2.zip

# kubectl for deployment to Amazon EKS.
ARG KUBECTL_VERSION=v1.34.2

RUN curl --fail --show-error --location \
      --retry 5 --retry-all-errors --retry-delay 10 \
      --connect-timeout 30 --max-time 300 \
      "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
      -o /usr/local/bin/kubectl \
    && chmod +x /usr/local/bin/kubectl \
    && kubectl version --client

# Jenkins plugins.
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins
