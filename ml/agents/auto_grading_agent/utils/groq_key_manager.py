import os
from pydantic import SecretStr

def load_groq_keys(filepath=os.path.join("groq_keys.txt")):
    with open(filepath, "r") as f:
        keys = [line.strip() for line in f if line.strip()]
    return keys

class GroqKeyManager:
    def __init__(self, filepath=os.path.join("groq_keys.txt")):
        self.keys = [SecretStr(k) for k in load_groq_keys(filepath)]
        self.idx = 0

    def get_key(self) -> SecretStr:
        if self.idx < len(self.keys):
            return self.keys[self.idx]
        else:
            raise RuntimeError("No more Groq API keys available.")

    def switch_key(self):
        self.idx += 1
        if self.idx < len(self.keys):
            return self.keys[self.idx]
        else:
            raise RuntimeError("No more Groq API keys available.")