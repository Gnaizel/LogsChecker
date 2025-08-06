FROM python:3.9-slim-buster

WORKDIR /app

COPY src/requirements.txt .


RUN if [ -f requirements.txt ]; then pip install -r requirements.txt; fi

COPY .. .

CMD ["python", "AssetMenagger/ScripteTG/src/resources/main.py"]