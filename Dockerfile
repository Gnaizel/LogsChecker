FROM ubuntu:latest
LABEL authors="Gnaizel"

ENTRYPOINT ["top", "-b"]