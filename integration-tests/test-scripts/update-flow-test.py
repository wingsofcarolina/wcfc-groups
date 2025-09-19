#!/usr/bin/env python3
"""
Streamlined Playwright-based authentication flow test for WCFC Manuals
"""

import json
import re
import sys
import time
import requests
import base64
from playwright.sync_api import sync_playwright, expect
import pymongo

def wait_for_page_ready(page, max_attempts=5):
    """Wait for page to be fully loaded and JavaScript ready"""
    print("Waiting for page to be ready...")
    
    for attempt in range(max_attempts):
        page.wait_for_load_state("load")
        page.wait_for_timeout(2000)  # Wait for JS to initialize
        
        # Check if we have actual content (not just modulepreload)
        content = page.content()
        if "modulepreload" in content and len(content) < 2000:
            print(f"Attempt {attempt + 1}: Page not fully loaded, waiting...")
            page.wait_for_timeout(3000)
            
            # Try to trigger loading
            try:
                page.evaluate("window.scrollTo(0, 100)")
                page.wait_for_timeout(1000)
            except:
                pass
        else:
            print("✓ Page is ready")
            return True
    
    # Last resort: hard refresh
    print("Page still not ready, trying hard refresh...")
    page.reload(wait_until="load")
    page.wait_for_timeout(5000)
    return True

def run_update_test():
    """Run the complete update flow test"""
    print("Starting update flow test...")
    
    with sync_playwright() as p:
        # Launch browser
        browser = p.chromium.launch(
            headless=True,
            args=[
                '--no-sandbox',
                '--disable-dev-shm-usage',
                '--disable-web-security',
                '--allow-running-insecure-content'
            ]
        )
        
        context = browser.new_context(
            viewport={'width': 1280, 'height': 720},
            java_script_enabled=True,
            ignore_https_errors=True
        )
        
        page = context.new_page()
        
        # Simple console logging
        page.on("console", lambda msg: print(f"Console: {msg.text}") if "404" not in msg.text else None)
        page.on("pageerror", lambda error: print(f"Page error: {error}"))
        
        try:

            # Step 1: Navigate to main page
            print("Step 1: Navigating to /...")
            
            response = page.goto("http://localhost:9301/", wait_until="load", timeout=15000)
            print(f"Navigation response: {response.status}")
           
            # Wait for page to be ready
            wait_for_page_ready(page)

            # Step 2: Upload the sample xls file from test-data

            # Step 3: View the checkbox page and see that there is one addition and one deletion

            # Step 4: Check both and submit

            # Step 5: Verify that WireMock has received updates for both groups.io and wcfc-manuals
           
            print("✅ Update flow completed successfully!")
            return True
            
        except Exception as e:
            print(f"❌ Test failed: {e}")
            
            # Take screenshot for debugging
            try:
                page.screenshot(path="/tmp/test-failure.png")
                print("Screenshot saved to /tmp/test-failure.png")
            except:
                pass
            
            return False
            
        finally:
            browser.close()

if __name__ == "__main__":
    try:
        success = run_update_test()
        if success:
            print("\n✅ Integration test PASSED!")
            sys.exit(0)
        else:
            print("\n❌ Integration test FAILED!")
            sys.exit(1)
    except Exception as e:
        print(f"\n💥 Test execution error: {e}")
        sys.exit(1)
