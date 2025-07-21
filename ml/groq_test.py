from groq import Groq
import os

KEYS_PATH = ""




def load_groq_keys(filepath=os.path.join("groq-keys/groq_keys.txt")):
    with open(filepath, "r") as f:
        keys = [line.strip() for line in f if line.strip()]
    return keys

class GroqKeyManager:
    def __init__(self, filepath=os.path.join("groq-keys/groq_keys.txt")):
        self.keys = load_groq_keys(filepath)
        self.idx = 0

    def get_key(self):
        if self.idx < len(self.keys):
            return self.keys[self.idx]
        else:
            raise RuntimeError("No more Groq API keys available.")

    def switch_key(self):
        self.idx += 1
        if self.idx < len(self.keys):
            os.environ["GROQ_API_KEY"] = self.keys[self.idx]
            return self.keys[self.idx]
        else:
            raise RuntimeError("No more Groq API keys available.")


groq_key_manager = GroqKeyManager(os.path.join(KEYS_PATH, "groq_keys.txt"))
os.environ["GROQ_API_KEY"] = groq_key_manager.get_key()


def get_groq_client():
    return Groq(api_key=os.environ["GROQ_API_KEY"])

def generate_groq(prompt, model_name):
    client = get_groq_client()

    chat_completion = client.chat.completions.create(
        messages=prompt,
        model=model_name,
    )

    return chat_completion.choices[0].message.content

resp = generate_groq([{"role":"user", "content": "Hi"}], "llama-3.3-70b-versatile")
print(resp)