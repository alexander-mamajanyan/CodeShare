import importlib.util
import pathlib
import sys
import unittest


HELPER_PATH = pathlib.Path(__file__).parents[2] / "app" / "src" / "main" / "python" / "myhelper.py"
SPEC = importlib.util.spec_from_file_location("codeshare_myhelper", HELPER_PATH)
helper = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(helper)


class PythonExecutorTests(unittest.TestCase):
    def test_runs_standard_library_and_captures_output(self):
        output = helper.main("import math\nprint(round(math.sqrt(81)))")
        self.assertEqual(output, "9\n")

    def test_provides_stdin_to_input(self):
        output = helper.main("print(input().upper())", "hello\n")
        self.assertEqual(output, "HELLO\n")

    def test_parses_function_arguments_as_python_literals(self):
        code = "def solution(values, multiplier):\n    return sum(values) * multiplier"
        result = helper.execute_method(code, "solution", ["[1, 2, 3]", "2"])
        self.assertEqual(result, "12")

    def test_splits_comma_separated_arguments_from_one_test_input(self):
        result = helper.execute_method(
            "def solution(left, right):\n    return left + right",
            "solution",
            ["2, 3"])
        self.assertEqual(result, "5")

    def test_preserves_an_explicit_tuple_as_one_argument(self):
        result = helper.execute_method(
            "def solution(pair):\n    return pair[0] + pair[1]",
            "solution",
            ["(2, 3)"])
        self.assertEqual(result, "5")

    def test_uses_printed_output_when_function_returns_none(self):
        result = helper.execute_method(
            "def solution():\n    print('done')", "solution", [])
        self.assertEqual(result, "done\n")

    def test_reports_runtime_errors_and_restores_standard_streams(self):
        original_stdout = sys.stdout
        output = helper.main("print(missing_name)")
        self.assertIn("NameError", output)
        self.assertIs(sys.stdout, original_stdout)


if __name__ == "__main__":
    unittest.main()
