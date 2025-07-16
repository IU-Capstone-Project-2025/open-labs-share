import ast
from typing import Tuple
from config import Config

class SecurityValidator:
    def __init__(self):
        self.allowed_imports = Config.ALLOWED_IMPORTS
        self.blocked_modules = Config.BLOCKED_MODULES
        self.max_code_length = Config.MAX_CODE_LENGTH

    def validate_code(self, code: str) -> Tuple[bool, str]:
        """Validate code for security concerns"""
        if len(code) > self.max_code_length:
            return False, f"Code exceeds maximum length of {self.max_code_length} characters"

        try:
            tree = ast.parse(code)
            result = self._validate_ast(tree)
            if not result[0]:
                return result
            return True, ""
        except SyntaxError as e:
            return False, f"Syntax error: {str(e)}"
        except Exception as e:
            return False, f"Validation error: {str(e)}"

    def _validate_ast(self, tree: ast.AST) -> Tuple[bool, str]:
        """Validate the AST for security concerns"""
        for node in ast.walk(tree):
            # Check imports
            if isinstance(node, (ast.Import, ast.ImportFrom)):
                result = self._validate_import(node)
                if not result[0]:
                    return result

            # Check for exec/eval calls
            elif isinstance(node, ast.Call):
                if isinstance(node.func, ast.Name) and node.func.id in ['exec', 'eval']:
                    return False, "Use of exec() or eval() is not allowed"

        return True, ""

    def _validate_import(self, node: ast.AST) -> Tuple[bool, str]:
        """Validate import statements"""
        if isinstance(node, ast.Import):
            for name in node.names:
                module = name.name.split('.')[0]
                if module in self.blocked_modules:
                    return False, f"Import of {module} is blocked"
                if module not in self.allowed_imports:
                    return False, f"Import of {module} is not allowed"
        
        elif isinstance(node, ast.ImportFrom):
            if node.module:
                module = node.module.split('.')[0]
                if module in self.blocked_modules:
                    return False, f"Import of {module} is blocked"
                if module not in self.allowed_imports:
                    return False, f"Import of {module} is not allowed"

        return True, ""
