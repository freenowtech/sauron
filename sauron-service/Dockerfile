FROM openjdk:11-jre-slim

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80" \
  JMXTRANS_VERSION=1.2.8

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
      build-essential \
      libreadline-gplv2-dev \
      libncursesw5-dev \
      libssl-dev \
      libsqlite3-dev \
      tk-dev \
      libgdbm-dev \
      libc6-dev \
      libbz2-dev \
      libffi-dev \
      zlib1g-dev

# Installing NodeJS
RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -

RUN apt-get update && apt-get install -y nodejs --no-install-recommends

# Installing Python3.9
RUN wget https://www.python.org/ftp/python/3.9.6/Python-3.9.6.tgz

RUN tar xzf Python-3.9.6.tgz && cd Python-3.9.6 && ./configure --enable-optimizations && make altinstall

RUN update-alternatives --install /usr/bin/python python /usr/local/bin/python3.9 10

RUN cd .. && rm -rf Python-3.9.6*

RUN rm -rf /var/lib/apt/lists/*

RUN mkdir /root/.m2

VOLUME /root/.m2

RUN mkdir /root/.gradle

VOLUME /root/.gradle

RUN mkdir -p /sauron/plugins

VOLUME /sauron/plugins

ENV TINI_VERSION v0.18.0

ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /usr/bin/tini

RUN chmod a+x /usr/bin/tini

COPY docker/entrypoint.sh /usr/local/bin/entrypoint

COPY target/sauron-service.jar /sauron

EXPOSE 8080

WORKDIR /sauron

ENTRYPOINT ["/usr/bin/tini", "--"]

CMD ["/usr/local/bin/entrypoint"]
