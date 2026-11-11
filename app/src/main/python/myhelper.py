import sys
import importlib.util
import types
from io import StringIO

def main(code):
    # Capture stdout
    old_stdout = sys.stdout
    redirected_output = sys.stdout = StringIO()
    
    try:
        # Execute the code
        exec(code)
        sys.stdout = old_stdout
        return redirected_output.getvalue()
    except Exception as e:
        sys.stdout = old_stdout
        return str(e)

def run_with_input(code, input_data):
    # Capture stdout and provide input
    old_stdout = sys.stdout
    old_stdin = sys.stdin
    redirected_output = sys.stdout = StringIO()
    sys.stdin = StringIO(input_data)
    
    try:
        # Execute the code
        exec(code)
        sys.stdout = old_stdout
        sys.stdin = old_stdin
        return redirected_output.getvalue()
    except Exception as e:
        sys.stdout = old_stdout
        sys.stdin = old_stdin
        return str(e)

# Create a dynamic module for code execution
def create_module_from_code(code):
    # Create a new module
    module_name = "user_solution"
    spec = importlib.util.spec_from_loader(module_name, loader=None)
    user_module = importlib.util.module_from_spec(spec)
    
    # Add the module to sys.modules
    sys.modules[module_name] = user_module
    
    try:
        # Execute the code in the module's namespace
        exec(code, user_module.__dict__)
        return user_module
    except Exception as e:
        return str(e)

# Alias function for compatibility with PythonExecutor
def execute_method(code, method_name, inputs):
    return run_method(code, method_name, inputs)

def run_method(code, method_name, inputs):
    try:
        # Create a module from the user's code
        module = create_module_from_code(code)
        
        # Check if module creation failed
        if isinstance(module, str):
            return f"Error in code: {module}"
        
        # Debug: Print all available functions in module
        available_functions = [name for name, obj in module.__dict__.items() 
                              if callable(obj) and not name.startswith('__')]
        debug_info = f"Available functions: {available_functions}\nLooking for: {method_name}"
        print(debug_info)  # This will show in the Android logs
        
        # Get the method from module
        if method_name not in module.__dict__:
            # Check if this is a case sensitivity issue
            method_found = False
            for name in module.__dict__:
                if name.lower() == method_name.lower() and callable(module.__dict__[name]):
                    print(f"Found function with different case: {name} instead of {method_name}")
                    method_name = name  # Use the actual name with correct case
                    method_found = True
                    break
            
            if not method_found:
                return f"Error: Function '{method_name}' not found. Please check your function name and make sure it matches exactly.\n\nAvailable functions: {available_functions}"
        
        method = module.__dict__[method_name]
        
        # Convert Java ArrayList to Python list and process inputs
        processed_inputs = []
        if hasattr(inputs, 'toArray'):  # Check if it's a Java ArrayList
            inputs = list(inputs.toArray())  # Convert Java ArrayList to Python list
            
        for input_str in inputs:
            try:
                # First try to convert to int or float directly
                try:
                    if '.' in str(input_str):
                        processed_input = float(input_str)
                    else:
                        processed_input = int(input_str)
                    processed_inputs.append(processed_input)
                    continue
                except (ValueError, TypeError):
                    pass
                
                # If not a simple number, try to evaluate as Python literal
                processed_input = eval(str(input_str))
                if isinstance(processed_input, list):
                    processed_input = list(processed_input)
                processed_inputs.append(processed_input)
            except:
                # If all else fails, keep as string
                processed_inputs.append(str(input_str))
        
        # Call the method with the inputs
        result = method(*processed_inputs)
        return str(result)
    except Exception as e:
        return f"Error: {str(e)}"
