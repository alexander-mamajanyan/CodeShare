import { python, pythonLanguage } from "@codemirror/lang-python";
import { oneDark } from "@codemirror/theme-one-dark";
import { EditorState } from "@codemirror/state";
import { EditorView } from "@codemirror/view";
import { classHighlighter, highlightTree } from "@lezer/highlight";
import { basicSetup } from "codemirror";

let view;

const editorTheme = EditorView.theme({
    "&": {
        height: "100%",
        color: "#F5F7FF",
        backgroundColor: "#0B1020",
        fontSize: "14px"
    },
    ".cm-content": {
        fontFamily: "monospace",
        caretColor: "#B8F36B",
        padding: "14px 0"
    },
    ".cm-gutters": {
        backgroundColor: "#151D30",
        color: "#7886A3",
        border: "none"
    },
    ".cm-activeLineGutter": {
        backgroundColor: "#1C2740"
    },
    ".cm-activeLine": {
        backgroundColor: "#151D30"
    },
    ".cm-cursor": {
        borderLeftColor: "#B8F36B"
    },
    ".cm-selectionBackground, ::selection": {
        backgroundColor: "#34415D !important"
    }
}, { dark: true });

function createEditor() {
    const editorElement = document.getElementById("editor");
    if (view || !editorElement) return;

    view = new EditorView({
        state: EditorState.create({
            doc: "# Write your Python solution here\n",
            extensions: [basicSetup, python(), oneDark, editorTheme, EditorView.lineWrapping]
        }),
        parent: editorElement
    });

    window.editor = {
        getValue: () => view.state.doc.toString(),
        setValue: value => view.dispatch({
            changes: { from: 0, to: view.state.doc.length, insert: value || "" }
        }),
        replaceSelection: value => view.dispatch(view.state.replaceSelection(value))
    };

    document.querySelectorAll("[data-insert]").forEach(button => {
        button.addEventListener("click", () => {
            view.focus();
            view.dispatch(view.state.replaceSelection(button.dataset.insert));
        });
    });

    document.querySelectorAll("[data-wrap]").forEach(button => {
        button.addEventListener("click", () => {
            const [before, after] = button.dataset.wrap.split("|");
            const selection = view.state.selection.main;
            const selectedText = view.state.sliceDoc(selection.from, selection.to);
            const insertedText = `${before}${selectedText}${after}`;
            view.dispatch({
                changes: { from: selection.from, to: selection.to, insert: insertedText },
                selection: {
                    anchor: selectedText
                        ? selection.from + insertedText.length
                        : selection.from + before.length
                }
            });
            view.focus();
        });
    });
}

function escapeHtml(text) {
    return text.replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;"
    })[character]);
}

window.highlightPythonCodeBlocks = root => {
    const scope = root || document;
    scope.querySelectorAll("pre code:not([data-python-highlighted])").forEach(code => {
        const source = code.textContent || "";
        const tree = pythonLanguage.parser.parse(source);
        const tokens = [];
        highlightTree(tree, classHighlighter, (from, to, className) => {
            tokens.push({ from, to, className });
        });

        let cursor = 0;
        let html = "";
        tokens.forEach(token => {
            if (token.from < cursor) return;
            html += escapeHtml(source.slice(cursor, token.from));
            html += `<span class="${token.className}">${escapeHtml(source.slice(token.from, token.to))}</span>`;
            cursor = token.to;
        });
        html += escapeHtml(source.slice(cursor));
        code.innerHTML = html;
        code.dataset.pythonHighlighted = "true";
    });
};

window.addLessonPlaygroundButtons = root => {
    const scope = root || document;
    scope.querySelectorAll("pre:not([data-playground-button])").forEach(pre => {
        const code = pre.querySelector("code");
        if (!code) return;

        const row = document.createElement("div");
        row.className = "lesson-playground-row";
        const button = document.createElement("button");
        button.type = "button";
        button.className = "lesson-playground-button";
        button.textContent = "Try it Yourself >>>";
        button.addEventListener("click", () => {
            if (window.LessonAndroid
                    && typeof window.LessonAndroid.openPlayground === "function") {
                window.LessonAndroid.openPlayground(code.textContent || "");
            }
        });
        row.appendChild(button);
        pre.insertAdjacentElement("afterend", row);
        pre.dataset.playgroundButton = "true";
    });
};

window.initializeEditor = createEditor;
window.getCode = () => window.editor ? window.editor.getValue() : "";
window.sendCode = () => {
    const code = window.getCode();
    if (window.Android && typeof window.Android.sendCodeToAndroid === "function") {
        window.Android.sendCodeToAndroid(code);
    }
    return code;
};

createEditor();
