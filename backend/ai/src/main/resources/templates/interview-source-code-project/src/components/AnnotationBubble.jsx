export default function AnnotationBubble({ label, detail, lineNum }) {
  return (
    <div data-annotation={lineNum}
      className="shrink-0 max-w-[220px] ml-3 px-3 py-1.5 bg-[#0f3460] border border-[#4ecca3] rounded-lg text-xs opacity-0"
    >
      <div className="text-[#4ecca3] font-medium">{label}</div>
      {detail && <div className="text-gray-400 mt-0.5 leading-relaxed">{detail}</div>}
    </div>
  )
}
