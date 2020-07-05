function select_one(status){
    if(status["status"] === "start"){ return "[<span style='color:#5080d9'>info</span>] Startup installation"; };
    if(status["status"] === "progress") { return "[<span style='color:green'>ok</span>]" + status["value"]; };
    if(status["status"] === "error") { return "[<span style='color:red'>error</span>]" + status["value"]; };
    if(status["status"] === "end"){ document.getElementById("menu").innerHTML = "<span style=\"background:#EEEEEE; padding:2px; position:fixed; right: 0px;\" onclick=\"window.external.invoke('some');\"><b><span style=\"color: #e32095\"> X </span></b>close</span>" ;
    return "[<span style='color:#5080d9'>info</span>] finish installation"; }
    return "[underfinied stage: " + status["status"] + "]";
}


function render(status){
  var app = document.getElementById("app");
  var sss = select_one(status);
  app.innerHTML = app.outerHTML + sss + "<br/>";
  app.scrollIntoView({ behavior: 'smooth', block: 'end' });
}
