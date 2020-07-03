//'use strict';
//
//function UI(items) {
//  var h = picodom.h;
//  function submit(e) {
//    e.preventDefault();
//    e.stopImmediatePropagation();
//    var el = document.getElementById('task-name-input');
//    rpc.addTask(el.value);
//    el.value = '';
//  }
//  function clearTasks() { rpc.clearDoneTasks(); }
//  function markTask(i){return function() { rpc.markTask(i, !items[i].done); }};
//
//  var taskItems = [];
//  for (var i = 0; i < items.length; i++) {
//    var checked = (items[i].done ? 'checked' : 'unchecked');
//    taskItems.push(
//	h('div', {className : 'task-item ' + checked, onclick : markTask(i)},
//	  items[i].name));
//  }
//
//  return h('div', {className : 'container'},
//	   h('form', {className : 'text-input-wrapper', onsubmit : submit},
//	     h('input', {
//	       id : 'task-name-input',
//	       className : 'text-input',
//	       type : 'text',
//	       autofocus : true
//	     })),
//	   h('div', {className : 'task-list'}, taskItems),
//	   h('div', {className : 'footer'},
//	     h('div', {className : 'btn-clear-tasks', onclick : clearTasks},
//	       'Delete completed')));
//}
//
//var element;
//var oldNode;
//var rpc = {
//  invoke : function(arg) { window.external.invoke(JSON.stringify(arg)); },
//  init : function() { rpc.invoke({cmd : 'init'}); },
//  log : function() {
//    var s = '';
//    for (var i = 0; i < arguments.length; i++) {
//      if (i != 0) {
//	s = s + ' ';
//      }
//      s = s + JSON.stringify(arguments[i]);
//    }
//    rpc.invoke({cmd : 'log', text : s});
//  },
//  addTask : function(name) { rpc.invoke({cmd : 'addTask', name : name}); },
//  clearDoneTasks : function() { rpc.invoke({cmd : 'clearDoneTasks'}); },
//  markTask : function(index, done) {
//    rpc.invoke({cmd : 'markTask', index : index, done : done});
//  },
//  render : function(items) {
//    return element = picodom.patch(oldNode, (oldNode = UI(items)), element);
//  },
//};
//
//window.onload = function() { rpc.init(); };

function select_one(status){
    if(status["status"] === "start"){ return "[<span style='color:#5080d9'>info</span>] Startup installation"; };
    if(status["status"] === "progress") { return "[<span style='color:green'>ok</span>]" + status["value"]; };
    if(status["status"] === "error") { return "[<span style='color:red'>error</span>]" + status["value"]; };
    if(status["status"] === "end"){ document.getElementById("menu").innerHTML = "<span style=\"background:#383838; padding:2px; position:fixed; right: 0px;\" onclick=\"window.external.invoke('some');\"><b><span style=\"color: #e32095\"> X </span></b>close</span>" ;
    return "[<span style='color:#5080d9'>info</span>] finish installation"; }
    return "[underfinied stage: " + status["status"] + "]";
}


function render(status){
  var app = document.getElementById("app");
  var sss = select_one(status);
  app.innerHTML = app.outerHTML + sss + "<br/>";
  app.scrollIntoView({ behavior: 'smooth', block: 'end' });
}
