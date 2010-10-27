#!/usr/bin/python

import socket
import sys

def print_help():
	print "Usage: listener.py <HOST> <PORT>"
	print "   HOST - ip address of local host"
	print "   PORT - local port number"

if __name__ == "__main__":
	if len(sys.argv) < 3:
		print_help()
		exit(1)

	LHOST = sys.argv[1]
	LPORT = int( sys.argv[2] )

	sock = socket.socket( socket.AF_INET, socket.SOCK_DGRAM )
	sock.bind( (LHOST, LPORT) )

	while True:
		data, addr = sock.recvfrom(1024)
		sys.stdout.write( data )
		sys.stdout.flush()

