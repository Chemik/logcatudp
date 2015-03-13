# Logcat to UDP

Simple android development tool that send log to UDP port. Background service is collecting logs and sending them to destination IP and UDP port. You can send logs to desktop through home wifi network, or to server with public IP or cname. You can use your desktop syslog server to receive logs (see below).

Or simply share your log with favorite application (eg. gmail).

Receive logs from your phone even if it is not connected to computer.

Available in [Google Play](https://play.google.com/store/apps/details?id=sk.madzik.android.logcatudp)

For discussion use [this mailing list](http://groups.google.com/group/logcatudp-discuss).

## HOWTO Recieve Logs

### Introduction

So you have installed application on your android device. That's great :)

But how you receive and/or save sended logs on computer?

Here are some ways how to do this:


#### 1. Read log direct from UDP port

To listen logs use listener.py script located in test directory

Example:
```
$ ./listener.py 192.168.1.10 10009
```

To save this output to file use standard linux/unix commands like output redirection or [tee](http://unixhelp.ed.ac.uk/CGI/man-cgi?tee).


#### 2. Default syslog server

If you use some kind of syslog server (rsyslog, SyslogNG), you can setup LogcatUDP to send logs to this server.

**Rsyslog** can be configured to listen remote logs on UDP port. Default port is 514. To enable listening uncomment this two lines in `/etc/rsyslog.conf`:
```
$ModLoad imudp
$UDPServerRun 514
```
You can of course change the number of port. With this configuration syslog writes log to number of log files (/var/log/syslog, /var/log/messages, ...). If you want to filter log lines from android and write them only to one special file, you must set up syslog rules. Create file in `/etc/rsyslog.d/` with name that is in front of other files in this directory. For example if the first file is `20-uwf.conf`, then you can name it `10-logcat.conf`. Write this rule to it:
```
#### process remote messages
if $fromhost-ip startswith '192.168.2.' then /var/log/android
& ~
# only local messages past this point
```
The third line (`"& ~"`) is important. This is discard message after write to file.
