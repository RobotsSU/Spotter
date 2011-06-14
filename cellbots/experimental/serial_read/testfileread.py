import time

file = open('/sdcard/ase/scripts/output.txt', 'r')

while 1:
  time.sleep(1)
  
  where = file.tell()
  line = file.readline()
  if not line:
    time.sleep(1)
    file.seek(where)
  else:
    print line, # already has newline
                                          
 
