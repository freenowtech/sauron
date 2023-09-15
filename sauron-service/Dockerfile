FROM openjdk:11-jre-slim

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80"

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
      libncursesw5-dev \
      libssl-dev \
      libsqlite3-dev \
      tk-dev \
      libgdbm-dev \
      libc6-dev \
      libbz2-dev \
      libffi-dev \
      zlib1g-dev \
      python3-dev \
      libpcap-dev \
      libpq-dev \
      libldap2-dev \
      libsasl2-dev \
      ldap-utils

# Installing NodeJS
RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -

RUN apt-get update && apt-get install -y nodejs --no-install-recommends

# Installing Python3.11
RUN wget https://www.python.org/ftp/python/3.11.4/Python-3.11.4.tgz

RUN tar xzf Python-3.11.4.tgz && cd Python-3.11.4 && ./configure --enable-optimizations && make altinstall

RUN update-alternatives --install /usr/bin/python python /usr/local/bin/python3.11 10

RUN cd .. && rm -rf Python-3.11.4*

RUN rm -rf /var/lib/apt/lists/*

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

ENV TINI_VERSION v0.18.0

ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /usr/bin/tini

RUN chmod a+x /usr/bin/tini

COPY docker/entrypoint.sh /usr/local/bin/entrypoint

COPY target/sauron-service.jar /sauron

EXPOSE 8080

WORKDIR /sauron

ENTRYPOINT ["/usr/bin/tini", "--"]

CMD ["/usr/local/bin/entrypoint"]
