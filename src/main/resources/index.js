/**
 * Created by mac on 2017/6/18.
 */

function onLoad() {
    console.log("On load.");
    alert("On load.");
    document.getElementById("div2").style.width = '200px';
    document.getElementById("div2").style.background = 'blue';
    console.log('hljs', hljs);
    alert("hljs");
    alert(hljs);
    hljs.initHighlightingOnLoad();
}

function onClick() {
    console.log('Clicked');
    console.log('undefined=');
    console.log(undefined);
    console.log("hljs");
    console.log(hljs);
    hljs.highlightBlock(document.getElementsByName("code")[0]);
    hljs.highlightBlock(document.getElementsByName("pre")[0]);
}

function renderCode(code) {
    container = document.getElementById("container");
    container.innerText = code;
    hljs.highlightBlock(container);
}