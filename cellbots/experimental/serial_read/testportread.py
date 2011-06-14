import os, time, socket, select

svr_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
svr_sock.bind(('', 9002))
svr_sock.listen(3)
svr_sock.setblocking(0)

rs = []


while 1:
  r,w,_ = select.select([svr_sock] + rs, [], [])
  print "out of select loop"
  
  for cli in r:
    if cli == svr_sock:
      new_cli,addr = svr_sock.accept()
      rs = [new_cli]
    else:   
      msg = cli.recv(1024)
      print "received: %s" % msg
      os.system("echo '%s\n' > /dev/ttyMSM2" % msg)
      time.sleep(1)
      os.system("echo 's\n' > /dev/ttyMSM2")
