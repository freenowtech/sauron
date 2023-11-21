FROM openjdk:11-jre-slim

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

ENV PYTHON_VERSION="3.11.4"

ENV NODE_VERSION="18"

LABEL maintainer="upscaler@free-now.com"

RUN ln -s $(which java) /usr/bin/java

RUN mkdir -p /usr/share/man/man1

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl \
      bash \
      procps \
      maven \
      git \
      ssh \
      apt-utils \
      wget \
      gpg \
      gcc \
      libc6-dev \
      pkg-config \
      build-essential \
      libssl-dev \
      zlib1g-dev \
      libbz2-dev \
      libreadline-dev \
      libsqlite3-dev \
      libncursesw5-dev \
      xz-utils \
      tk-dev \
      libxml2-dev \
      libxmlsec1-dev \
      libffi-dev \
      liblzma-dev

# Installing NodeJS
RUN mkdir -p /etc/apt/keyrings && \
      curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
      | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
      echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_$NODE_VERSION.x nodistro main" \
      > /etc/apt/sources.list.d/nodesource.list && \
      apt-get update && apt-get install -y nodejs --no-install-recommends && rm -rf /var/lib/apt/lists/*

# Installing Python
RUN curl -s https://pyenv.run | bash && eval "$(/root/.pyenv/bin/pyenv init -)" && \
      /root/.pyenv/bin/pyenv install -v "$PYTHON_VERSION"

RUN update-alternatives --install /usr/bin/python python /root/.pyenv/versions/"$PYTHON_VERSION"/bin/python 10

# Installing Poetry
RUN python -m pip install poetry==1.1.7

# Upgrade Pip
RUN python -m pip install --upgrade pip

RUN mkdir /root/.m2

VOLUME /root/.m2

RUN mkdir /root/.gradle

VOLUME /root/.gradle

RUN mkdir /root/.pip

VOLUME /root/.pip

RUN mkdir -p /sauron/plugins

VOLUME /sauron/plugins

ENV TINI_VERSION v0.19.0

# Defined by Docker, see https://docs.docker.com/desktop/extensions-sdk/extensions/multi-arch/#adding-multi-arch-binaries
ARG TARGETARCH

ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-${TARGETARCH} /usr/bin/tini

RUN chmod a+x /usr/bin/tini

COPY docker/entrypoint.sh /usr/local/bin/entrypoint

COPY target/sauron-service.jar /sauron

EXPOSE 8080

WORKDIR /sauron

ENTRYPOINT ["/usr/bin/tini", "--"]

CMD ["/usr/local/bin/entrypoint"]
