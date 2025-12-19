FROM python:3.11-slim

RUN pip install --no-cache-dir pygments pytest

WORKDIR /app

COPY syntax/pygments/message_format.py /app/syntax/pygments/message_format.py
COPY syntax/pygments/__init__.py /app/syntax/pygments/__init__.py
COPY syntax/__init__.py /app/syntax/__init__.py
COPY syntax/pygments/test/test_messageformat_lexer.py /app/syntax/pygments/test/test_messageformat_lexer.py

ENV PYTHONPATH=/app

CMD ["pytest", "-v", "/app/syntax/pygments/test/test_messageformat_lexer.py"]
