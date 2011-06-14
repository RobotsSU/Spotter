function getRobotsOnlineLoop()
{
getRobotsOnline();
t=setTimeout("getRobotsOnlineLoop()",10000);
}

function sendCommand(botmsg, botname){
  var url="sendmsg";
  url=url+"?msg="+botmsg;
  url=url+"&botname="+botname;
  url=url+"&sid="+Math.random();
  if (window.XMLHttpRequest)
  {// code for IE7+, Firefox, Chrome, Opera, Safari
    xmlhttp=new XMLHttpRequest();
  }
  else
  {// code for IE6, IE5
    xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
  }
    xmlhttp.onreadystatechange=function()
  {
    if (xmlhttp.readyState==4 && xmlhttp.status==200)
    {
      robotresponses = document.getElementById('robotresponses');
      robotresponses.innerHTML = xmlhttp.responseText + "<br>" + robotresponses.innerHTML; 
    }
  }
  robotresponses = document.getElementById('robotresponses');
  robotresponses.innerHTML = "About to send to " + botname + ": " + botmsg + "<br>" + robotresponses.innerHTML; 
  xmlhttp.open("GET",url,true);
  xmlhttp.send();  
}

function getRobotsOnline(){
  if (window.XMLHttpRequest)
  {// code for IE7+, Firefox, Chrome, Opera, Safari
    xmlhttp=new XMLHttpRequest();
  }
  else
  {// code for IE6, IE5
    xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
  }
    xmlhttp.onreadystatechange=function()
  {
    if (xmlhttp.readyState==4 && xmlhttp.status==200)
    {
      robotsonline = document.getElementById('robotsonline');
      robotsonline.innerHTML = xmlhttp.responseText;
    }
  }
  xmlhttp.open("GET","robotsonline",true);
  xmlhttp.send();  
}
