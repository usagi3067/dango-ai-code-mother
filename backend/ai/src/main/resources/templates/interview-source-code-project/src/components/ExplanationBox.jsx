export default function SpeechBox({ speech, note }) {
  return (
    <div className="bg-[#0f3460] rounded-lg p-5 border-l-4 border-[#e94560]">
      <div className="text-base text-gray-200 leading-relaxed">
        <span className="text-[#e94560] mr-2">🎤</span>
        <span dangerouslySetInnerHTML={{ __html: speech }} />
      </div>
      {note && (
        <div className="text-xs text-gray-500 mt-3 pt-2 border-t border-[#1a4a7a]">
          {note}
        </div>
      )}
    </div>
  )
}
