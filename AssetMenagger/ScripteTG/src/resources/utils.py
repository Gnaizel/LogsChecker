import os
import random
import shutil
import string
from pathlib import Path
from typing import Dict, Union

from telethon.tl.types import Message

from objects.logger import LoggerObject
from objects.links import LinkType

LOGGER = None


def get_paths() -> Dict[str, str]:
    """
    Returns a dictionary of paths to files
    :returns dict[str, str]: paths to files
    """

    resources_dir: str = os.path.dirname(os.path.abspath(__file__))

    # dict with program paths
    return {
        "RESOURCES_PATH": resources_dir,
        "SESSIONS_UPLOAD_PATH": f"{resources_dir}{os.sep}..{os.sep}sessions{os.sep}upload",
        "SESSIONS_NOT_WORK_PATH": f"{resources_dir}{os.sep}..{os.sep}sessions{os.sep}not_work",
        "DATABASE_PATH": f"{resources_dir}{os.sep}data{os.sep}soft.db",
        "FILE_DOWNLOAD_DIR": f"C:/files"
#          "FILE_DOWNLOAD_DIR": f"{resources_dir}{os.sep}..{os.sep}files"
    }


def get_logger(time: bool = True, debug: bool = True) -> LoggerObject:
    """
    Get logger object
    :param debug: bool - enable debug
    :param time: bool - enable time in log
    :return: LoggerObject: logger object
    """

    global LOGGER

    # if logger not init
    if not LOGGER:
        return LoggerObject(time=time, debug=debug)
    else:

        return LOGGER


def move_file(from_path: str, to_path: str) -> None:
    """
    Move file from one path to another path
    :param from_path: str - path to file
    :param to_path: str - path to file
    :return:
    """
    shutil.move(from_path, to_path)


def id_generator(size: int = 8, chars: str = string.ascii_lowercase + string.digits):
    """
    Random string generation
    :param size: int - number of characters
    :param chars: str - used characters in string
    :return: generate random of string with given size and characters
    """
    return ''.join(random.choice(chars) for _ in range(size))


def get_link_type(link: str, link_type: LinkType = None) -> Union[LinkType, str]:
    """
    Get the type of link or transform it to a specified type.

    :param link: The input link.
    :param link_type: The desired LinkType to transform the link to.
    :return: The LinkType of the input link or the transformed link based on link_type.
    """

    if not link:
        # Если строка пуста, возвращаем LinkType.NONE
        return LinkType.NONE

    if link_type is None or not link.startswith((LinkType.HTTPS.value, LinkType.TELEGRAM.value, LinkType.DOG.value)):
        # Если link_type не указан или строка не начинается с нужных префиксов, возвращаем LinkType.NONE
        return LinkType.NONE

    current_link_type = get_link_type(link=link)

    if link_type is current_link_type:
        # Если link_type совпадает с текущим типом, возвращаем оригинальную строку
        return link

    # Убираем лишние префиксы для дальнейшего форматирования
    link = link.replace("https://t.me/", "").replace("t.me/", "").replace("@", "")

    # Форматирование строки в соответствии с указанным link_type
    if link_type is LinkType.DOG:
        return f"@{link}"
    elif link_type is LinkType.HTTPS:
        return f"https://t.me/{link}"
    elif link_type is LinkType.TELEGRAM:
        return f"t.me/{link}"

    # Если ни одно из условий не выполнено, возвращаем LinkType.NONE
    return LinkType.NONE


def sizeof_fmt(num: int, suffix: str = "B") -> str:
    for unit in ["", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "Zi"]:
        if abs(num) < 1024.0:
            return f"{num:3.1f}{unit}{suffix}"
        num /= 1024.0
    return f"{num:.1f}Yi{suffix}"