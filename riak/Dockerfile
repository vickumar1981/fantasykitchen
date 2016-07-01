FROM ubuntu
RUN apt-get update && apt-get install -y curl apt-transport-https
RUN curl http://apt.basho.com/gpg/basho.apt.key | apt-key add -
RUN echo "deb http://apt.basho.com precise main" | tee -a /etc/apt/sources.list
RUN apt-get update && apt-get install -y riak
RUN sed -i 's/riak_kv_bitcask_backend/riak_kv_eleveldb_backend/g' /etc/riak/app.config
RUN sed -i 's/127.0.0.1/0.0.0.0/g' /etc/riak/app.config
RUN service riak restart
EXPOSE 8098 8087


