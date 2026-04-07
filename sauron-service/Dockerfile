FROM eclipse-temurin:11-jre AS java-dist
FROM node:24-slim AS node-dist
FROM anchore/syft:v1.42.3 AS syft-dist

# Consolidated Tooling Image with Python, Node, and Syft (SBOM Generator)
FROM python:3.11-slim AS sauron-tooling

# Copy Node.js from official image
COPY --from=node-dist /usr/local/bin /usr/local/bin
COPY --from=node-dist /usr/local/lib /usr/local/lib

# Copy Syft from official image
COPY --from=syft-dist /syft /usr/local/bin/syft

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl \
      bash \
      procps \
      git \
      ssh \
      ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN python -m pip install --upgrade pip && \
    python -m pip install --user pipx && \
    python -m pipx ensurepath && \
    /root/.local/bin/pipx install poetry==1.8.2 && \
    /root/.local/bin/pipx inject poetry poetry-plugin-export==1.7.1 && \
    /root/.local/bin/pipx install cyclonedx-bom==4.1.5 && \
    ln -s /root/.local/bin/poetry /usr/local/bin/poetry && \
    ln -s /root/.local/bin/cyclonedx-py /usr/local/bin/cyclonedx-py

ENV PATH="/usr/local/bin:${PATH}"


# Final image - Inherits tools and adds Java, Maven, and service configs
FROM sauron-tooling AS sauron-service

# Copy Java from official image
COPY --from=java-dist /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME="/opt/java/openjdk"
RUN ln -s "${JAVA_HOME}/bin/java" /usr/bin/java

ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

RUN mkdir -p /usr/share/man/man1 && \
    apt-get update && apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

ENV TINI_VERSION v0.19.0
ARG TARGETARCH
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-${TARGETARCH} /usr/bin/tini
RUN chmod a+x /usr/bin/tini

RUN mkdir /root/.m2
VOLUME /root/.m2

RUN mkdir /root/.gradle
VOLUME /root/.gradle

RUN mkdir /root/.pip
VOLUME /root/.pip

RUN mkdir -p /sauron/plugins
VOLUME /sauron/plugins

COPY docker/entrypoint.sh /usr/local/bin/entrypoint
COPY target/sauron-service.jar /sauron

EXPOSE 8080
WORKDIR /sauron

ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["/usr/local/bin/entrypoint"]
