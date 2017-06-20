/**
 * Created by mac on 2017/6/18.
 */

function onLoad() {
    console.log("On load.");
}

function onClick() {
    hljs.highlightBlock(document.getElementsByName("code")[0]);
}

function renderCode(code) {
    const container = document.getElementById("container");
    container.innerText = code;
    hljs.highlightBlock(container);
    hljs.lineNumbersBlock(container);
}