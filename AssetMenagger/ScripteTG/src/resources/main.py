import asyncio
import json
import logging
import os
import traceback
from typing import Dict, List

from art import tprint

import utils
from database.models import register_models
from objects.logger import LoggerObject
from objects.session import WorkerSession

PATHS: Dict[str, str] = utils.get_paths()
LOGGER: LoggerObject = utils.get_logger()
SESSIONS: List[WorkerSession] = []

loop = asyncio.new_event_loop()


def main():

    # красивое лого))
    tprint(text="SCRIPTE TG", space=1, font="big")

    sessions_list = os.listdir(PATHS.get('SESSIONS_UPLOAD_PATH'))

    # если папка с сессиями пустая
    if len(sessions_list) < 1:
        LOGGER.error("Не обнаружены сессии в папке sessions/upload.")
        exit(0)

    sessions_path = PATHS.get('SESSIONS_UPLOAD_PATH')
    sessions_list = os.listdir(sessions_path)

    # если папка с сессиями пустая
    if len(sessions_list) < 1:
        LOGGER.error(f"Не обнаружены сессии в папке {sessions_path}.")
        exit(0)

    # проверяем все сессии
    for session_name in sessions_list:
        if not session_name.endswith("json"):
            continue

        with open(f"{sessions_path}{os.sep}{session_name}", "r") as file:
            data = json.load(file)

        session_file = session_name.replace('.json', '')

        LOGGER.log(f"Загружена сессия {session_file}")

        if data.get('proxy'):
            proxies = (
                int(data.get('proxy')[0]),
                data.get('proxy')[1],
                int(data.get('proxy')[2]),
                bool(data.get('proxy')[3]),
                data.get('proxy')[4],
                data.get('proxy')[5]
            )
        else:
            proxies = None

        sess: WorkerSession = WorkerSession(
            app_id=data.get("app_id"),
            app_hash=data.get("app_hash"),
            path_to_session=f"{sessions_path}{os.sep}{session_file}.session",
            device=data.get("device"),
            app_version=data.get("app_version"),
            lang_pack=data.get("lang_pack"),
            system_lang_pack=data.get("system_lang_pack"),
            two_fa=data.get('twoFA'),
            proxy=proxies,
            log=True,
            logger=LOGGER
        )

        is_valid: bool = asyncio.get_event_loop().run_until_complete(sess.valid(set_username=False))

        if is_valid:
            SESSIONS.append(sess)

    LOGGER.info(f"Загружено {len(SESSIONS)} рабочих сессий.")

    for session in SESSIONS:
        # запускаем на сессии хандлеры
        asyncio.get_event_loop().run_until_complete(session.register_handlers())


if __name__ == '__main__':

    # иницилизируем базу данных
    register_models()

    try:
        main()
    except KeyboardInterrupt:
        LOGGER.info("Stopped by CTRL+C")
