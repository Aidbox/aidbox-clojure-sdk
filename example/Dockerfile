FROM clojure:tools-deps
RUN mkdir /app
WORKDIR /app

ADD ./deps.edn /app/deps.edn
# Fetch dependencies (cache them on this step)
RUN clj -A:nrepl:test -e "(System/exit 0)"

ADD . /app

EXPOSE 55555
