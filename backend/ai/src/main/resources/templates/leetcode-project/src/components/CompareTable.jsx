import { Table, Tag } from 'antd'

const levelColor = { good: 'green', medium: 'orange', bad: 'red' }

const columns = [
  { title: '解法', dataIndex: 'name', key: 'name', render: t => <span className="font-bold text-white">{t}</span> },
  {
    title: '时间复杂度',
    dataIndex: 'timeComplexity',
    key: 'time',
    render: c => <Tag color={levelColor[c.level]}>{c.value}</Tag>,
  },
  {
    title: '空间复杂度',
    dataIndex: 'spaceComplexity',
    key: 'space',
    render: c => <Tag color={levelColor[c.level]}>{c.value}</Tag>,
  },
  { title: '核心思想', dataIndex: 'idea', key: 'idea' },
  { title: '优点', dataIndex: 'pros', key: 'pros', render: t => <span className="text-[#4ecca3]">{t}</span> },
  { title: '缺点', dataIndex: 'cons', key: 'cons', render: t => <span className="text-[#e94560]">{t}</span> },
]

export default function CompareTable({ comparison }) {
  return (
    <Table
      columns={columns}
      dataSource={comparison.map((item, i) => ({ ...item, key: i }))}
      pagination={false}
      size="middle"
      className="[&_.ant-table]:!bg-transparent [&_.ant-table-thead_th]:!bg-[#0f3460] [&_.ant-table-tbody_td]:!border-b-[#1a3a5c] [&_.ant-table-row:hover_td]:!bg-[#1a3a5c]"
    />
  )
}
