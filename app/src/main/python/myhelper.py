import ast
import sys
import threading
import traceback
from io import StringIO


_execution_lock = threading.RLock()


def main(code, input_data=""):
    with _execution_lock:
        old_stdout = sys.stdout
        old_stdin = sys.stdin
        captured_output = StringIO()
        sys.stdout = captured_output
        sys.stdin = StringIO(input_data or "")

        try:
            namespace = {"__name__": "__main__"}
            exec(compile(code, "<codeshare>", "exec"), namespace, namespace)
            return captured_output.getvalue()
        except Exception:
            return traceback.format_exc()
        finally:
            sys.stdout = old_stdout
            sys.stdin = old_stdin


def execute_method(code, method_name, inputs):
    with _execution_lock:
        old_stdout = sys.stdout
        old_stdin = sys.stdin
        captured_output = StringIO()
        sys.stdout = captured_output
        sys.stdin = StringIO()

        try:
            namespace = {"__name__": "user_solution"}
            exec(compile(code, "<codeshare>", "exec"), namespace, namespace)
            method = namespace.get(method_name)
            if not callable(method):
                return "Error: Function '{}' not found.".format(method_name)

            if hasattr(inputs, "toArray"):
                inputs = list(inputs.toArray())

            values = [str(value) for value in (inputs or [])]
            parsed_inputs = []
            if len(values) == 1:
                try:
                    parsed_value = ast.literal_eval(values[0])
                except (ValueError, SyntaxError):
                    parsed_inputs.append(values[0])
                else:
                    if isinstance(parsed_value, tuple) and "," in values[0] and not values[0].lstrip().startswith("("):
                        parsed_inputs.extend(parsed_value)
                    else:
                        parsed_inputs.append(parsed_value)
            else:
                for value in values:
                    try:
                        parsed_inputs.append(ast.literal_eval(value))
                    except (ValueError, SyntaxError):
                        parsed_inputs.append(value)

            result = method(*parsed_inputs)
            if result is None:
                return captured_output.getvalue()
            return str(result)
        except Exception:
            return "Error: " + traceback.format_exc()
        finally:
            sys.stdout = old_stdout
            sys.stdin = old_stdin
