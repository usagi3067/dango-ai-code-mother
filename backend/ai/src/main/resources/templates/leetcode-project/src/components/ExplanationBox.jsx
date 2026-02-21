export default function ExplanationBox({ explanation }) {
  return (
    <div className="bg-[#0f3460] rounded-lg p-4 border-l-4 border-[#e94560] text-sm text-gray-300 leading-relaxed">
      <span dangerouslySetInnerHTML={{ __html: explanation }} />
    </div>
  )
}
