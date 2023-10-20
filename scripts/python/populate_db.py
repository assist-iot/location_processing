import asyncio
import random as r
import string
from argparse import ArgumentParser
from dataclasses import dataclass
from pprint import pprint

import httpx
import uvloop
from tqdm.asyncio import tqdm


def generate_random_string(min_len=0, max_len=50):
    length = r.randint(min_len, max_len)
    return "".join(r.choice(string.ascii_letters) for _ in range(length))


def generate_random_query():
    data = {"name": generate_random_string(min_len=5), "sql": generate_random_string()}

    # input settings
    if r.random() < 0.5:
        data["inputSettings"] = {
            "host": "mosquitto",
            "port": 1883,
        }
        if r.random() < 0.5:
            data["inputSettings"]["password"] = generate_random_string()
        if r.random() < 0.5:
            data["inputSettings"]["username"] = generate_random_string()
        if r.random() < 0.5:
            num_topics = r.randint(0, 10)
            data["inputSettings"]["topics"] = [
                {"name": generate_random_string(min_len=1)} for _ in range(num_topics)
            ]

    # output settings
    if r.random() < 0.5:
        data["outputSettings"] = {
            "host": "mosquitto",
            "port": 1883,
            "format": {
                "recordFormat": r.choice(["object", "array"]),
                "showHeader": r.choice([True, False]),
                "wrapSingleColumn": r.choice([True, False]),
            },
        }
        if r.random() < 0.5:
            data["outputSettings"]["password"] = generate_random_string()
        if r.random() < 0.5:
            data["outputSettings"]["username"] = generate_random_string()
        if r.random() < 0.5:
            num_topics = r.randint(0, 10)
            data["outputSettings"]["topics"] = []
            for _ in range(num_topics):
                topic = {"name": generate_random_string(min_len=1)}
                publish_flags = []
                if r.random() < 0.5:
                    flags = [
                        "QoSAtLeastOnceDelivery",
                        "QoSAtMostOnceDelivery",
                        "QoSExactlyOnceDelivery",
                    ]
                    publish_flags = r.sample(flags, k=1)
                    if r.random() < 0.5:
                        publish_flags.append("Retain")
                topic["publishFlags"] = publish_flags
                topic["publishWhen"] = r.choice(["always", "success", "failure"])
                topic["publishEmptyOutput"] = r.choice([True, False])
                data["outputSettings"]["topics"].append(topic)

    return data


def generate_same_query():
    return {
        "name": generate_random_string(min_len=10) + "_SAME",
        "sql": "select concat({strInput},"
        + f" '{generate_random_string(min_len=10)}')"
        + ";",
        "inputSettings": {
            "password": "password",
            "username": "username",
            "host": "mosquitto",
            "port": 1883,
            "topics": [{"name": "input"}],
        },
        "outputSettings": {
            "password": "password",
            "username": "username",
            "host": "mosquitto",
            "port": 1883,
            "topics": [
                {
                    "name": f"output/{generate_random_string(min_len=1)}",
                    "publishFlags": ["QoSAtLeastOnceDelivery", "Retain"],
                    "publishWhen": "always",
                    "publishEmptyOutput": True,
                }
            ],
            "format": {
                "recordFormat": "object",
                "showHeader": True,
                "wrapSingleColumn": True,
            },
        },
    }


async def create_query(url, type):
    if type == "random":
        query = generate_random_query()
    elif type == "same":
        query = generate_same_query()
    else:
        raise ValueError(f"Unknown type: {type}")
    try:
        async with httpx.AsyncClient(timeout=None) as client:
            response = await client.post(url, json=query)
            if response.status_code == 201:
                return True
            else:
                pprint(response.text)
    except Exception as e:
        pprint(e)
    pprint(query)
    return False


async def create_queries(url, num_queries, concurrency, type):
    semaphore = asyncio.Semaphore(concurrency)

    async def rate_limited_query():
        async with semaphore:
            return await create_query(url, type)

    tasks = [asyncio.create_task(rate_limited_query()) for _ in range(num_queries)]
    results = await tqdm.gather(*tasks)
    return sum(results)


@dataclass
class Args:
    url: str
    num_queries: int
    concurrency: int
    type: str


def get_args():
    parser = ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080/v1/queries")
    parser.add_argument("--num_queries", default=1, type=int)
    parser.add_argument("--concurrency", default=1, type=int)
    parser.add_argument("--type", default="random", choices=["random", "same"])
    args = parser.parse_args()
    return Args(**vars(args))


async def main():
    args = get_args()
    pprint(f"Running for {args}")
    result = await create_queries(
        args.url, args.num_queries, args.concurrency, args.type
    )
    pprint(f"Created {result}/{args.num_queries} queries")


if __name__ == "__main__":
    uvloop.install()
    asyncio.run(main())
