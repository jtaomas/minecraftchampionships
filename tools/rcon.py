import socket
import struct
import sys


def pack(req_id, req_type, body):
    payload = struct.pack("<ii", req_id, req_type) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def recv_exact(sock, n):
    out = b""
    while len(out) < n:
        chunk = sock.recv(n - len(out))
        if not chunk:
            raise EOFError("connection closed")
        out += chunk
    return out


def read_packet(sock):
    length = struct.unpack("<i", recv_exact(sock, 4))[0]
    data = recv_exact(sock, length)
    req_id, req_type = struct.unpack("<ii", data[:8])
    body = data[8:-2].decode("utf-8", "replace")
    return req_id, req_type, body


def main():
    host = "127.0.0.1"
    port = 25576
    password = "MccReviewRcon2026"
    command = " ".join(sys.argv[1:])
    with socket.create_connection((host, port), timeout=15) as sock:
        sock.sendall(pack(1, 3, password))
        req_id, req_type, body = read_packet(sock)
        if req_id == -1:
            raise SystemExit("RCON auth failed")
        sock.sendall(pack(2, 2, command))
        req_id, req_type, body = read_packet(sock)
        print(body)


if __name__ == "__main__":
    main()
