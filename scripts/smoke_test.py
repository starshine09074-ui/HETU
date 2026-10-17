#!/usr/bin/env python3
"""Minimal frontend, login, and authenticated Hetu API smoke test."""
import json
import sys
import urllib.error
import urllib.request

FRONTEND = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:5173").rstrip("/")
API = (sys.argv[2] if len(sys.argv) > 2 else "http://127.0.0.1:8080").rstrip("/")
USERNAME = sys.argv[3] if len(sys.argv) > 3 else "admin"
PASSWORD = sys.argv[4] if len(sys.argv) > 4 else "admin123"


def get(url, headers=None):
    req = urllib.request.Request(url, headers=headers or {})
    with urllib.request.urlopen(req, timeout=20) as response:
        return response.status, response.read()


status, page = get(FRONTEND + "/")
assert status == 200 and "河图".encode() in page, "frontend home page did not return Hetu HTML"
print("PASS frontend home page")

status, captcha_payload = get(API + "/captchaImage")
captcha = json.loads(captcha_payload)
assert status == 200 and captcha.get("code") == 200, "captcha/login bootstrap endpoint failed"
assert captcha.get("captchaEnabled") is False, "smoke test expects CAPTCHA disabled only in disposable CI database"
print("PASS login bootstrap endpoint")

body = json.dumps({"username": USERNAME, "password": PASSWORD}).encode()
request = urllib.request.Request(API + "/login", data=body,
                                 headers={"Content-Type": "application/json"}, method="POST")
with urllib.request.urlopen(request, timeout=20) as response:
    login = json.loads(response.read())
assert login.get("code") == 200 and login.get("token"), f"login failed: {login}"
print("PASS login API")

token = login["token"]
status, projects_payload = get(API + "/hetu/projects", {"Authorization": "Bearer " + token})
projects = json.loads(projects_payload)
assert status == 200 and projects.get("code") == 200, f"core projects API failed: {projects}"
assert projects.get("data"), "core projects API returned no seeded project"
print("PASS authenticated core projects API")
