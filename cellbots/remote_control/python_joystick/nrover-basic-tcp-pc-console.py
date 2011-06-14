#!/usr/bin/python
# Copyright(c) 2008 Patrick Engelman
# This code is public domain.
# Note, this requires the pygame library, and listens on
# port 17171 -- if you are connecting from outside a firewall
# with your phone you need to make your firewall forward port 17171 to your PC.


import string,sys,pygame
from socket import *
from pygame.locals import *
from time import sleep

# Socket stuff

# Old UDP code
#phoneaddr = "32.139.144.108"
#phoneport = 17171
#phonesock = socket(AF_INET,SOCK_DGRAM)
# send using phonesock.sendto("hexstring",phoneaddr)
phonesock = socket(AF_INET, SOCK_STREAM)
phonesock.bind(('',17171))
phonesock.listen(5)



# Begin of joystick/pygame code
pygame.init()
screen= pygame.display.set_mode((640, 480), 0, 32)
font = pygame.font.Font(None, 25)
serva="\xff\x00\x80"
servb="\xff\x00\x80"
servc="\xff\x00\x80"
joysticks = []
for joystick_no in xrange(pygame.joystick.get_count()):
    stick = pygame.joystick.Joystick(joystick_no)
    stick.init()
    joysticks.append(stick)

if not joysticks:
    print "No joysticks found."
    exit()

active_joystick = 1

pygame.display.set_caption(joysticks[active_joystick].get_name())

def draw_axis(surface, x,y, axis_x, axis_y, size):
    line_col = (128,128,128)
    num_lines = 40
    step = size / float(num_lines)
    for n in xrange(num_lines):
        line_col = [(192, 192, 192),(220, 220, 220)][n&1]
        pygame.draw.line(surface, line_col, (x+n*step, y), (x+n*step, y+size))
        pygame.draw.line(surface, line_col, (x, y+ n*step), (x+size, y+n*step))

    pygame.draw.line(surface, (0,0,0), (x, y+size/2), (x+size, y+size/2))
    pygame.draw.line(surface, (0,0,0), (x+size/2, y), (x+size/2, y+size))

    draw_x = int(x + (axis_x * size + size ) / 2.)
    draw_y = int(y + (axis_y * size + size ) / 2.)
    draw_pos = (draw_x, draw_y)
    center_pos = (x+size/2, y+size/2)
    pygame.draw.line(surface, (0,0,0), center_pos, draw_pos, 5)
    pygame.draw.circle(surface, (0, 0, 255), draw_pos, 10)


def draw_dpad(surface, x, y,  axis_x, axis_y):

    col = (255, 0, 0)
    if (axis_x == -1):
        pygame.draw.circle(surface, col, (x-20, y), 10)
    elif axis_x == +1:
        pygame.draw.circle(surface, col, (x+20, y), 10)

    if (axis_y ==-1):
        pygame.draw.circle(surface, col, (x, y+20), 10)
    elif axis_y == +1:
        pygame.draw.circle(surface, col, (x, y-20), 10)
motocmd = 0
while True:
    connection, address = phonesock.accept()
    while True:
        if True:
            joystick = joysticks[active_joystick]

            for event in pygame.event.get():
                if event.type == QUIT:
                    pygame.joystick.quit()
                    pygame.display.quit()
                    sys.exit()
                if event.type == pygame.QUIT:
                    pygame.joystick.quit()
                    pygame.display.quit()
                    sys.exit()
                if event.type == KEYDOWN:
                    if event.key >= K_0 and event.key <= K_1:
                        num = event.key - K_0
                        if num < len(joysticks):
                            active_joystick = num
                            name = joysticks[active_joystick].get_name()
                            pygame.display.set_caption(name)

            axes = []
            for axis_no in xrange(joystick.get_numaxes()):
                axes.append( joystick.get_axis(axis_no) )

            axis_size = min( 256, 640 / (joystick.get_numaxes()/2))

            pygame.draw.rect(screen, (255, 255, 255), (0, 0, 640, 480))

            x = 0
            for axis_no in xrange(0, len(axes), 2):
                axis_x = axes[axis_no]
                if axis_no+1 < len(axes):
                    axis_y = axes[axis_no + 1]
                else:
                    axis_y = 0
             
                text= font.render("%s   %s   %s " % (motocmd,axis_x,axis_y),1, (10,10,10))
                if axis_no == 0:
                    # send messages to move first 2 servos
                    # print "\xff\x00%c is the code for %s" % (int(axis_x * 128) + 128, int(axis_x * 128) + 128)
                    serva = "\xff\x02%c" % (int(axis_x * -128) + 128)
                    #phonesock.sendto("\xff\x01",phoneaddr)
                    if  axis_y < 0 :
                        motocmd = (int(axis_y * 64) + 110)
                    if  axis_y > 0 :
                        motocmd = (int(axis_y * 64) + 142)
		    if  axis_y == 0:
                        motocmd = 128
                    servb = "\xff\x01%c" %(motocmd)
                if axis_no == 1:
                    #phonesock.sendto("\xff\x02",(phoneaddr,phoneport))
                    servc = "\xff\x03%c" % (int(axis_x * 128) + 128)
                textpos = text.get_rect()
                textpos.centerx = screen.get_rect().centerx
                textpos.centery= 200 + (axis_no * 25)
                screen.blit(text,textpos)
                draw_axis(screen, x, 0, axis_x, axis_y, axis_size)
                x += axis_size
            sleep(.05)
            connection.send(serva)
            connection.send(servb)
            connection.send(servc)
            pygame.display.update()
        else:
            break
    phonesock.close()
