# Makefile - Chuẩn hóa lệnh điều phối dự án RoomSync
.PHONY: install test test-be test-fe verify dev dev-be dev-fe build clean help

help:
	@echo "Danh sách lệnh điều phối dự án RoomSync:"
	@echo "  make install    - Cài đặt dependencies cho root và frontend"
	@echo "  make verify     - Chạy toàn bộ test suites (Backend JUnit 5 + Frontend Vitest)"
	@echo "  make test       - Chạy kiểm thử mặc định"
	@echo "  make test-be    - Chạy kiểm thử Backend Java Spring Boot"
	@echo "  make test-fe    - Chạy kiểm thử Frontend React Vitest"
	@echo "  make dev        - Khởi chạy đồng thời BE (port 8080) và FE (port 5173)"
	@echo "  make build      - Đóng gói cả Backend và Frontend"
	@echo "  make clean      - Dọn dẹp build artifacts"

install:
	npm install
	cd fe && npm install

verify:
	npm run verify

test:
	npm test

test-be:
	npm run test:be

test-fe:
	npm run test:fe

dev:
	npm run dev

dev-be:
	npm run dev:be

dev-fe:
	npm run dev:fe

build:
	npm run build

clean:
	cd be && .\mvnw.cmd clean
