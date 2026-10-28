FROM python:3.12-slim
WORKDIR /app
COPY requirements.lock .
RUN pip install --no-cache-dir -r requirements.lock && useradd --create-home --uid 10001 vitalday
COPY vitalday ./vitalday
USER vitalday
EXPOSE 8000
CMD ["uvicorn", "vitalday.app:create_app", "--factory", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
