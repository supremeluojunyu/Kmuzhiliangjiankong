import Quill from 'quill';
import 'quill/dist/quill.snow.css';
import { useEffect, useRef } from 'react';

interface RichTextEditorProps {
  value?: string;
  onChange?: (html: string) => void;
  placeholder?: string;
  readOnly?: boolean;
  minHeight?: number;
}

const TOOLBAR = [
  ['bold', 'italic', 'underline'],
  [{ list: 'ordered' }, { list: 'bullet' }],
  ['link'],
  ['clean'],
];

export default function RichTextEditor({
  value = '',
  onChange,
  placeholder,
  readOnly = false,
  minHeight = 160,
}: RichTextEditorProps) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<HTMLDivElement>(null);
  const quillRef = useRef<Quill | null>(null);
  const onChangeRef = useRef(onChange);

  onChangeRef.current = onChange;

  useEffect(() => {
    if (!editorRef.current || quillRef.current) {
      return;
    }

    const quill = new Quill(editorRef.current, {
      theme: 'snow',
      placeholder,
      readOnly,
      modules: { toolbar: TOOLBAR },
    });

    quill.on('text-change', () => {
      const html = quill.root.innerHTML;
      const empty = html === '<p><br></p>' || html === '<p></p>';
      onChangeRef.current?.(empty ? '' : html);
    });

    if (value) {
      quill.root.innerHTML = value;
    }

    quillRef.current = quill;

    return () => {
      quillRef.current = null;
      if (wrapperRef.current) {
        const toolbar = wrapperRef.current.querySelector('.ql-toolbar');
        toolbar?.remove();
      }
    };
  }, []);

  useEffect(() => {
    const quill = quillRef.current;
    if (!quill) return;
    const current = quill.root.innerHTML;
    const next = value || '';
    if (next !== current && next !== (current === '<p><br></p>' ? '' : current)) {
      quill.root.innerHTML = next;
    }
  }, [value]);

  useEffect(() => {
    quillRef.current?.enable(!readOnly);
  }, [readOnly]);

  return (
    <div ref={wrapperRef} className="rich-text-editor">
      <div ref={editorRef} style={{ minHeight }} />
    </div>
  );
}
