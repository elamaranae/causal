#!/usr/bin/env python3
"""Generates per-service seed SQL files from seed-data.yml."""

import json
import os
import random
import yaml

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


def load_config():
    with open(os.path.join(SCRIPT_DIR, "seed-data.yml")) as f:
        return yaml.safe_load(f)["seed"]


def sql_str(val):
    """Escape a string for SQL single-quote literals."""
    if val is None:
        return "NULL"
    return "'" + str(val).replace("'", "''") + "'"


def sql_header(db_name):
    return f"""\
-- {db_name} Database Seed (generated from seed-data.yml)
-- Do not edit manually — run: python3 generate-seeds.py

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SELECT pg_catalog.set_config('search_path', 'public', false);
"""


# ---------------------------------------------------------------------------
# Identity seed
# ---------------------------------------------------------------------------
def generate_identity_seed(cfg):
    lines = [sql_header("Identity")]
    lines.append("TRUNCATE public.refresh_tokens, public.users RESTART IDENTITY CASCADE;\n")
    for u in cfg["identity"]["users"]:
        lines.append(
            f"INSERT INTO public.users VALUES ({u['id']}, NULL, {sql_str(u['email'])}, "
            f"{sql_str(u['password_hash'])}, NULL);"
        )
    lines.append("")
    lines.append("SELECT setval('public.users_id_seq', 1, true);")
    lines.append("SELECT setval('public.refresh_tokens_id_seq', 1, true);")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# Product seed
# ---------------------------------------------------------------------------
def generate_product_seed(cfg):
    rng = random.Random(cfg["random_seed"])
    lines = [sql_header("Product")]
    lines.append(
        "TRUNCATE prices, reviews, skus, product_media, products, product_categories, vendors "
        "RESTART IDENTITY CASCADE;\n"
    )

    # Vendors
    lines.append("-- Vendors")
    vals = []
    for v in cfg["vendors"]:
        vals.append(
            f"({v['id']}, {sql_str(v['name'])}, {sql_str(v['description'])}, {sql_str(v['logo'])})"
        )
    lines.append("INSERT INTO vendors (id, name, description, logo) VALUES")
    lines.append(",\n".join(vals) + ";")
    lines.append(f"SELECT setval('vendors_id_seq', {cfg['vendors'][-1]['id']});\n")

    # Categories
    lines.append("-- Categories")
    vals = []
    for c in cfg["categories"]:
        parent = str(c["parent_id"]) if "parent_id" in c else "NULL"
        vals.append(
            f"({c['id']}, {sql_str(c['name'])}, {sql_str(c['description'])}, {parent})"
        )
    lines.append("INSERT INTO product_categories (id, name, description, parent_id) VALUES")
    lines.append(",\n".join(vals) + ";")
    lines.append(f"SELECT setval('product_categories_id_seq', {cfg['categories'][-1]['id']});\n")

    # Resolve variant config with overrides
    base_variants = cfg["variants"]
    overrides = cfg.get("variant_overrides", {})

    def get_variant_config(category_id, variant_key):
        base = dict(base_variants.get(variant_key, {}))
        cat_overrides = overrides.get(category_id, overrides.get(str(category_id), {}))
        if variant_key in cat_overrides:
            base.update(cat_overrides[variant_key])
        return base

    # Products, SKUs, Media, Prices
    product_id = 0
    sku_id = 0
    media_id = 0
    price_id = 0

    product_rows = []
    sku_rows = []
    media_rows = []
    price_rows = []

    currencies = cfg["currencies"]["rates"]
    sku_product_map = []  # (sku_id, product_id) for inventory

    for cat_entry in cfg["catalog"]:
        cat_id = cat_entry["category_id"]
        vendor_id = cat_entry["vendor_id"]
        variant_key = cat_entry["variant_key"]
        min_skus, max_skus = cat_entry["skus_per_product"]
        min_price, max_price = [float(x) for x in cat_entry["price_range_usd"]]
        images = cat_entry["images"]
        vc = get_variant_config(cat_id, variant_key)
        primary_values = vc.get("values", [])
        secondary_key = vc.get("secondary_key")
        secondary_values = vc.get("secondary_values", [])

        for product_name in cat_entry["products"]:
            product_id += 1
            num_skus = rng.randint(min_skus, max_skus)
            thumb = rng.choice(images) + "?w=400&q=80"
            first_sku_of_product = sku_id + 1

            product_rows.append(
                f"({product_id}, {sql_str(product_name)}, NULL, "
                f"{vendor_id}, {cat_id}, {sql_str(variant_key)}, {sql_str(thumb)})"
            )

            for j in range(num_skus):
                sku_id += 1
                media_id += 1

                # Variant attributes
                pv = primary_values[j % len(primary_values)] if primary_values else str(j)
                attrs = {variant_key: pv}
                if secondary_key and secondary_values:
                    attrs[secondary_key] = rng.choice(secondary_values)
                attrs_json = json.dumps(attrs)

                sku_thumb = rng.choice(images) + "?w=200&q=80"

                # Media
                media_img = rng.choice(images)
                media_json = json.dumps([{
                    "url": media_img + "?w=700&q=80",
                    "type": "IMAGE",
                    "primary": True,
                    "thumbnail": media_img + "?w=200&q=80",
                }])
                # Add 0-2 extra images
                extra_count = rng.randint(0, min(2, len(images) - 1))
                if extra_count > 0:
                    media_list = json.loads(media_json)
                    extra_imgs = rng.sample([i for i in images if i != media_img], extra_count)
                    for ei in extra_imgs:
                        media_list.append({
                            "url": ei + f"?w={rng.choice([800,900])}&q=80",
                            "type": "IMAGE",
                            "primary": False,
                            "thumbnail": ei + "?w=200&q=80",
                        })
                    media_json = json.dumps(media_list)

                media_rows.append(f"({media_id}, {sql_str(media_json)})")
                sku_rows.append(
                    f"({sku_id}, {product_id}, {sql_str(attrs_json)}::jsonb, {media_id}, {sql_str(sku_thumb)})"
                )
                sku_product_map.append((sku_id, product_id))

                # Prices in all currencies
                base_price = round(rng.uniform(min_price, max_price), 2)
                # Snap to .99
                base_price = int(base_price) + 0.99
                for curr, rate in currencies.items():
                    price_id += 1
                    amount = round(base_price * rate, 4) if curr != "USD" else base_price
                    price_rows.append(
                        f"({price_id}, {sku_id}, '2024-01-01', {sql_str(curr)}, {amount})"
                    )

            pass  # default_sku_id is set via UPDATE after inserts

    # Write products
    lines.append("-- Products")
    lines.append(
        "INSERT INTO products (id, name, description, vendor_id, category_id, "
        "primary_variant_key, primary_thumbnail_url) VALUES"
    )
    for i, row in enumerate(product_rows):
        lines.append(row + ("," if i < len(product_rows) - 1 else ";"))
    lines.append(f"SELECT setval('products_id_seq', {product_id});\n")

    # Write media in chunks
    lines.append("-- Product Media")
    chunk_size = 500
    for i in range(0, len(media_rows), chunk_size):
        chunk = media_rows[i : i + chunk_size]
        lines.append("INSERT INTO product_media (id, media) VALUES")
        for j, row in enumerate(chunk):
            lines.append(row + ("," if j < len(chunk) - 1 else ";"))
    lines.append(f"SELECT setval('product_media_id_seq', {media_id});\n")

    # Write SKUs in chunks
    lines.append("-- SKUs")
    for i in range(0, len(sku_rows), chunk_size):
        chunk = sku_rows[i : i + chunk_size]
        lines.append(
            "INSERT INTO skus (id, product_id, variant_attributes, media_id, primary_thumbnail_url) VALUES"
        )
        for j, row in enumerate(chunk):
            lines.append(row + ("," if j < len(chunk) - 1 else ";"))
    lines.append(f"SELECT setval('skus_id_seq', {sku_id});\n")

    # Set default_sku_id on products
    lines.append("-- Set default SKU for each product")
    # Build a mapping of product_id -> first sku_id
    prod_first_sku = {}
    for sid, pid in sku_product_map:
        if pid not in prod_first_sku:
            prod_first_sku[pid] = sid
    lines.append("UPDATE products SET default_sku_id = t.sku_id FROM (VALUES")
    vals = [f"({pid}, {sid})" for pid, sid in sorted(prod_first_sku.items())]
    for i in range(0, len(vals), 10):
        chunk = vals[i : i + 10]
        joined = ", ".join(chunk)
        lines.append(joined + ("," if i + 10 < len(vals) else ""))
    lines.append(") AS t(product_id, sku_id) WHERE products.id = t.product_id;\n")

    # Write prices in chunks
    lines.append("-- Prices")
    for i in range(0, len(price_rows), chunk_size):
        chunk = price_rows[i : i + chunk_size]
        lines.append(
            "INSERT INTO prices (id, sku_id, effective_from, price_currency, price_amount) VALUES"
        )
        for j, row in enumerate(chunk):
            lines.append(row + ("," if j < len(chunk) - 1 else ";"))
    lines.append(f"SELECT setval('prices_id_seq', {price_id});")

    return "\n".join(lines) + "\n", sku_product_map


# ---------------------------------------------------------------------------
# Inventory seed
# ---------------------------------------------------------------------------
def generate_inventory_seed(cfg, sku_product_map):
    rng = random.Random(cfg["random_seed"] + 1)  # different seed for inventory randomness
    dist = cfg["inventory"]["distribution"]
    lines = [sql_header("Inventory")]
    lines.append("TRUNCATE stocks RESTART IDENTITY CASCADE;\n")

    # Group SKUs by product for the temp table approach
    # Build ranges: consecutive sku_ids for same product_id
    ranges = []
    if sku_product_map:
        cur_pid = sku_product_map[0][1]
        cur_first = sku_product_map[0][0]
        cur_last = cur_first
        for sid, pid in sku_product_map[1:]:
            if pid == cur_pid and sid == cur_last + 1:
                cur_last = sid
            else:
                ranges.append((cur_first, cur_last, cur_pid))
                cur_pid = pid
                cur_first = sid
                cur_last = sid
        ranges.append((cur_first, cur_last, cur_pid))

    total_skus = len(sku_product_map)

    lines.append("-- SKU-to-product mapping derived from product catalog")
    lines.append(
        "CREATE TEMP TABLE sku_product_map (first_sku BIGINT, last_sku BIGINT, product_id BIGINT);"
    )
    lines.append("INSERT INTO sku_product_map (first_sku, last_sku, product_id) VALUES")
    for i, (first, last, pid) in enumerate(ranges):
        comma = "," if i < len(ranges) - 1 else ";"
        lines.append(f"({first}, {last}, {pid}){comma}")

    out_of_stock = dist["out_of_stock"]
    low_stock = dist["low_stock"]
    normal_stock = dist["normal_stock"]

    lines.append(f"""
DO $$
DECLARE
    r RECORD;
    v_sku_id BIGINT;
    v_quantity INT;
BEGIN
    FOR r IN SELECT first_sku, last_sku, product_id FROM sku_product_map ORDER BY first_sku LOOP
        FOR v_sku_id IN r.first_sku..r.last_sku LOOP
            IF random() < {out_of_stock} THEN
                v_quantity := 0;
            ELSIF random() < {out_of_stock + low_stock} THEN
                v_quantity := 1 + floor(random() * 10)::INT;
            ELSIF random() < {out_of_stock + low_stock + normal_stock} THEN
                v_quantity := 10 + floor(random() * 90)::INT;
            ELSE
                v_quantity := 100 + floor(random() * 400)::INT;
            END IF;

            INSERT INTO stocks (sku_id, product_id, quantity)
            VALUES (v_sku_id, r.product_id, v_quantity)
            ON CONFLICT (sku_id) DO NOTHING;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Inventory seed complete: % stock records created', {total_skus};
END $$;

DROP TABLE sku_product_map;""")

    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    cfg = load_config()

    identity_sql = generate_identity_seed(cfg)
    product_sql, sku_product_map = generate_product_seed(cfg)
    inventory_sql = generate_inventory_seed(cfg, sku_product_map)

    files = {
        "identity/seed.sql": identity_sql,
        "product/seed.sql": product_sql,
        "inventory/seed.sql": inventory_sql,
    }

    for path, content in files.items():
        full_path = os.path.join(SCRIPT_DIR, path)
        with open(full_path, "w") as f:
            f.write(content)
        print(f"  wrote {path} ({len(content):,} bytes)")

    print(
        f"\nGenerated {len(sku_product_map):,} SKUs across "
        f"{max(pid for _, pid in sku_product_map):,} products"
    )


if __name__ == "__main__":
    main()
