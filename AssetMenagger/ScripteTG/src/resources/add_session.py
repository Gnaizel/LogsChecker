import asyncio
import json
import os
from typing import Dict

import telethon.errors
from telethon import TelegramClient

import utils

PATHS: Dict[str, str] = utils.get_paths()


async def auth(phone: str, app_id: int, app_hash: str):
    client: TelegramClient = TelegramClient(
        f"{PATHS.get('SESSIONS_UPLOAD_PATH')}{os.sep}{phone}",
        api_id=app_id,
        api_hash=app_hash,
        system_version="4.16.30-vxCUSTOM",
        device_model="Xiaomi Redmi Note 7",
        app_version="0.21.8.1166-armeabi-v7a",
        lang_code="en",
        system_lang_code="en"
    )

    await client.connect()

    await client.send_code_request(phone=phone)

    code = input("Введите код, который вам пришёл: ")

    try:
        await client.sign_in(phone=phone, code=code)
    except telethon.errors.rpcerrorlist.SessionPasswordNeededError:
        two_fa = input("Введите TwoFA пароль: ")

        await client.sign_in(phone=phone, password=two_fa)

    data = {
        "session_file": phone,
        "phone": phone,
        "app_id": app_id,
        "app_hash": app_hash,
        "proxy": None
    }

    with open(f"{PATHS.get('SESSIONS_UPLOAD_PATH')}{os.sep}{phone}.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

    print("Сессия успешна добавлена.")


def main():
    # запрашиваем у пользователя номер телефона сессии
    phone = input("Введите номер телефона сессии: ").strip()
    app_id = int(input("Введите app_id сессии: ").strip())
    app_hash = input("Введите app_hash сессии: ").strip()

    asyncio.get_event_loop().run_until_complete(auth(phone, app_id, app_hash))


if __name__ == '__main__':
    main()
