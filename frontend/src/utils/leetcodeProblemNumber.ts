export const LEETCODE_PROBLEM_NUMBER_MIN = 1
export const LEETCODE_PROBLEM_NUMBER_MAX = 3000

export const isValidLeetCodeProblemNumber = (value: unknown): value is number => {
  return (
    typeof value === 'number' &&
    Number.isInteger(value) &&
    value >= LEETCODE_PROBLEM_NUMBER_MIN &&
    value <= LEETCODE_PROBLEM_NUMBER_MAX
  )
}
