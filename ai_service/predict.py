#!/usr/bin/env python3
"""
Demand prediction script for POS system.
Reads product sales data from stdin (JSON), computes predictions, writes JSON to stdout.

Input format (JSON array):
[
  {
    "productId": 1,
    "productName": "Product A",
    "totalSoldAllTime": 1500,
    "totalDays": 180,
    "currentStock": 20
  },
  ...
]

Output format:
{
  "predictions": [
    {
      "productId": 1,
      "productName": "Product A",
      "currentStock": 20,
      "avgDailySales": 5.0,
      "predictedDemand7Days": 35.0,
      "daysOfStockRemaining": 4.0,
      "restockStatus": "URGENT"
    },
    ...
  ]
}
"""

import sys
import json


def predict(products):
    predictions = []

    for p in products:
        product_id = p["productId"]
        product_name = p["productName"]
        total_sold = p.get("totalSoldAllTime", 0) or 0
        total_days = p.get("totalDays", 1) or 1
        current_stock = p.get("currentStock", 0) or 0

        avg_daily_sales = round(total_sold / float(total_days), 2)
        predicted_7_days = round(avg_daily_sales * 7, 1)

        if avg_daily_sales > 0:
            days_remaining = round(current_stock / avg_daily_sales, 1)
        else:
            days_remaining = 9999.0  # No sales history → no depletion

        if days_remaining < 5:
            restock_status = "URGENT"
        elif days_remaining < 14:
            restock_status = "LOW"
        else:
            restock_status = "OK"

        predictions.append({
            "productId": product_id,
            "productName": product_name,
            "currentStock": current_stock,
            "avgDailySales": avg_daily_sales,
            "predictedDemand7Days": predicted_7_days,
            "daysOfStockRemaining": days_remaining,
            "restockStatus": restock_status
        })

    # Sort by urgency: URGENT first, then LOW, then OK
    order = {"URGENT": 0, "LOW": 1, "OK": 2}
    predictions.sort(key=lambda x: order.get(x["restockStatus"], 3))

    return {"predictions": predictions}


if __name__ == "__main__":
    try:
        raw = sys.stdin.read()
        products = json.loads(raw)
        result = predict(products)
        print(json.dumps(result))
    except Exception as e:
        print(json.dumps({"error": str(e), "predictions": []}))
        sys.exit(1)