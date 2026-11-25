import json
import socket
import struct
import sys

host = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
port = int(sys.argv[2]) if len(sys.argv) > 2 else 25580


def varint(n):
    out = b""
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out += bytes([b | 0x80])
        else:
            out += bytes([b])
            return out


def read_varint(sock):
    num = 0
    shift = 0
    while True:
        b = sock.recv(1)
        if not b:
            raise EOFError
        v = b[0]
        num |= (v & 0x7F) << shift
        if not v & 0x80:
            return num
        shift += 7


def parse_status_packet(data):
    i = [0]

    def rv():
        num = 0
        shift = 0
        while True:
            v = data[i[0]]
            i[0] += 1
            num |= (v & 0x7F) << shift
            if not v & 0x80:
                return num
            shift += 7

    rv()  # packet id
    slen = rv()
    return data[i[0]:i[0] + slen].decode("utf-8", "replace")


with socket.create_connection((host, port), timeout=5) as s:
    host_b = host.encode()
    payload = varint(0x00) + varint(767) + varint(len(host_b)) + host_b + struct.pack(">H", port) + varint(1)
    s.sendall(varint(len(payload)) + payload)
    s.sendall(varint(1) + varint(0x00))
    length = read_varint(s)
    data = b""
    while len(data) < length:
        data += s.recv(length - len(data))
    text = parse_status_packet(data)
    print(text)
    try:
        obj = json.loads(text)
        print("PLAYERS", obj.get("players", {}).get("online"), obj.get("players", {}).get("sample"))
    except Exception as e:
        print("parse error", e)
