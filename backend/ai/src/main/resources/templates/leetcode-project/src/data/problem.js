export const problem = {
  number: 1,
  title: '两数之和',
  difficulty: 'easy',
  description: '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。',
  coreIdea: {
    steps: [
      {
        title: '问题本质',
        content: '在数组中找两个数 a 和 b，使得 a + b = target。',
      },
      {
        title: '关键洞察',
        content: '如果我们知道 a，那么 b = target - a 就确定了。问题转化为：遍历数组时，能否快速判断 complement = target - nums[i] 是否已经出现过？',
        formula: 'complement = target - nums[i]',
      },
      {
        title: '暴力思路',
        content: '双重循环，对每个 nums[i]，遍历后续元素找 nums[j] == target - nums[i]。时间 O(n²)。',
      },
      {
        title: '优化思路',
        content: '用哈希表存储已遍历的元素（值→索引）。遍历时先查哈希表中是否有 complement，有则直接返回。时间 O(n)，空间 O(n)。',
      },
    ],
    summary: '核心在于「空间换时间」：用哈希表将查找从 O(n) 降到 O(1)。',
  },
  comparison: [
    {
      name: '暴力枚举',
      timeComplexity: { value: 'O(n²)', level: 'bad' },
      spaceComplexity: { value: 'O(1)', level: 'good' },
      idea: '双重循环逐对检查',
      pros: '无需额外空间，思路直观',
      cons: '时间复杂度高，大数据量超时',
    },
    {
      name: '哈希表',
      timeComplexity: { value: 'O(n)', level: 'good' },
      spaceComplexity: { value: 'O(n)', level: 'medium' },
      idea: '一次遍历 + 哈希表查找 complement',
      pros: '时间最优，一次遍历即可',
      cons: '需要额外 O(n) 空间',
    },
  ],
}
