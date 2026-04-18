#!/usr/bin/env python3
"""
End-to-end integration test for WCFC Groups update flow
Includes WireMock setup and complete user workflow testing
"""

import json
import os
import re
import subprocess
import sys
import time
import requests
from playwright.sync_api import sync_playwright, expect

WARNING_PATTERN = re.compile(r"NOTE:.*groups\.io", re.DOTALL)

def setup_wiremock_stubs():
    """Setup WireMock stubs for Groups.io and WCFC-Manuals APIs"""
    print("Setting up WireMock stubs for Groups.io and WCFC-Manuals APIs...")
    
    wiremock_url = "http://localhost:8080"
    
    # Wait for WireMock to be ready
    for i in range(30):
        try:
            response = requests.get(f"{wiremock_url}/__admin/health")
            if response.status_code == 200:
                break
        except requests.exceptions.ConnectionError:
            pass
        time.sleep(1)
    else:
        raise Exception("WireMock is not ready")
    
    # Clear existing stubs
    requests.delete(f"{wiremock_url}/__admin/mappings")
    
    # Groups.io API - Add member endpoint stub
    groupsio_add_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/v1/directadd",
            "headers": {
                "Authorization": {
                    "matches": "Bearer .*"
                },
                "Content-Type": {
                    "equalTo": "application/x-www-form-urlencoded"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=groupsio_add_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create Groups.io add member stub: {response.text}")
    
    print("Created Groups.io add member stub")
    
    # Groups.io API - Remove member endpoint stub
    groupsio_remove_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/v1/bulkremovemembers",
            "headers": {
                "Authorization": {
                    "matches": "Bearer .*"
                },
                "Content-Type": {
                    "equalTo": "application/x-www-form-urlencoded"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=groupsio_remove_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create Groups.io remove member stub: {response.text}")
    
    print("Created Groups.io remove member stub")
    
    # WCFC-Manuals API - Add member endpoint stub
    manuals_add_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/member/add",
            "headers": {
                "X-WCFC-TOKEN": {
                    "matches": ".*"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=manuals_add_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create WCFC-Manuals add member stub: {response.text}")
    
    print("Created WCFC-Manuals add member stub")
    
    # WCFC-Manuals API - Remove member endpoint stub
    manuals_remove_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/api/member/remove",
            "headers": {
                "X-WCFC-TOKEN": {
                    "matches": ".*"
                }
            }
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=manuals_remove_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create WCFC-Manuals remove member stub: {response.text}")
    
    print("Created WCFC-Manuals remove member stub")
    
    print("WireMock stubs setup completed successfully!")

def get_api_requests():
    """Get all API requests sent to WireMock"""
    wiremock_url = "http://localhost:8080"
    
    response = requests.get(f"{wiremock_url}/__admin/requests")
    if response.status_code == 200:
        requests_data = response.json()
        api_requests = {
            'groupsio_add': [],
            'groupsio_remove': [],
            'manuals_add': [],
            'manuals_remove': []
        }
        
        all_requests = requests_data.get('requests', [])
        
        for request in all_requests:
            # The URL is nested inside request.request.url
            request_data = request.get('request', {})
            url = request_data.get('url', '')
            if '/api/v1/directadd' in url:
                api_requests['groupsio_add'].append(request)
            elif '/api/v1/bulkremovemembers' in url:
                api_requests['groupsio_remove'].append(request)
            elif '/api/member/add' in url:
                api_requests['manuals_add'].append(request)
            elif '/api/member/remove' in url:
                api_requests['manuals_remove'].append(request)
        
        return api_requests
    else:
        raise Exception(f"Failed to get requests from WireMock: {response.text}")

def reset_api_requests():
    """Clear WireMock request history while keeping configured stubs"""
    wiremock_url = "http://localhost:8080"
    response = requests.delete(f"{wiremock_url}/__admin/requests")
    if response.status_code not in (200, 204):
        raise Exception(f"Failed to clear WireMock requests: {response.text}")

def reset_mongodb_data():
    """Reset MongoDB to the common seed data for each scenario"""
    subprocess.run(["python3", "/app/test-scripts/setup-test-data.py"], check=True)

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

def verify_api_call_counts(expected_counts):
    """Verify that WireMock saw the expected API request counts"""
    time.sleep(3)
    api_requests = get_api_requests()

    actual_counts = {
        'groupsio_add': len(api_requests['groupsio_add']),
        'groupsio_remove': len(api_requests['groupsio_remove']),
        'manuals_add': len(api_requests['manuals_add']),
        'manuals_remove': len(api_requests['manuals_remove'])
    }

    print(f"API call summary:")
    print(f"  Groups.io add calls: {actual_counts['groupsio_add']}")
    print(f"  Groups.io remove calls: {actual_counts['groupsio_remove']}")
    print(f"  WCFC-Manuals add calls: {actual_counts['manuals_add']}")
    print(f"  WCFC-Manuals remove calls: {actual_counts['manuals_remove']}")

    for key, expected in expected_counts.items():
        actual = actual_counts[key]
        if actual != expected:
            raise Exception(f"Expected exactly {expected} {key} calls, found {actual}")

def run_browser_scenario(page, name, test_file_path, expected_warning, expected_checkboxes, expected_counts):
    """Run one browser upload/submit scenario"""
    print(f"Running scenario: {name}")
    reset_mongodb_data()
    reset_api_requests()

    print("Navigating to /...")
    response = page.goto("http://localhost:9301/", wait_until="load", timeout=15000)
    print(f"Navigation response: {response.status}")
    wait_for_page_ready(page)

    print(f"Uploading XLS file: {test_file_path}")
    file_input = page.locator('input[type="file"]')
    expect(file_input).to_be_visible(timeout=10000)
    file_input.set_input_files(test_file_path)

    print("Waiting for results page...")
    wait_for_page_ready(page)

    warning = page.get_by_text(WARNING_PATTERN)
    if expected_warning:
        expect(warning).to_be_visible(timeout=10000)
        print("✅ Expected changed-email warning is visible")
    else:
        expect(warning).to_have_count(0)
        print("✅ Changed-email warning is not visible")

    checkboxes = page.locator('input[type="checkbox"]')
    checkbox_count = checkboxes.count()
    print(f"Found {checkbox_count} checkboxes")
    if checkbox_count != expected_checkboxes:
        raise Exception(f"Expected {expected_checkboxes} checkboxes, but found {checkbox_count}")

    for index in range(expected_checkboxes):
        checkbox = checkboxes.nth(index)
        expect(checkbox).to_be_visible(timeout=10000)
        checkbox.check()

    submit_button = page.locator('button:has-text("Submit"), button:has-text("Apply"), input[type="submit"]').first
    expect(submit_button).to_be_visible(timeout=5000)
    submit_button.click()
    print("✅ Submit button clicked")
    wait_for_page_ready(page)

    verify_api_call_counts(expected_counts)
    print(f"✅ Scenario completed successfully: {name}")

def run_update_test():
    """Run the complete update flow test"""
    print("Starting update flow test...")
    
    # Setup WireMock stubs first
    try:
        setup_wiremock_stubs()
        print("✅ WireMock stubs setup completed")
    except Exception as e:
        print(f"❌ Failed to setup WireMock stubs: {e}")
        return False
    
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
            run_browser_scenario(
                page,
                "add/remove changes do not show changed-email warning",
                "/app/test-data/myfbo-report.xls",
                expected_warning=False,
                expected_checkboxes=2,
                expected_counts={
                    'groupsio_add': 1,
                    'groupsio_remove': 1,
                    'manuals_add': 1,
                    'manuals_remove': 1
                }
            )

            run_browser_scenario(
                page,
                "changed email shows warning and skips groups.io",
                "/app/test-data/myfbo-report-email-change.xls",
                expected_warning=True,
                expected_checkboxes=1,
                expected_counts={
                    'groupsio_add': 0,
                    'groupsio_remove': 0,
                    'manuals_add': 1,
                    'manuals_remove': 1
                }
            )

            print("✅ Update flow completed successfully!")
            return True
            
        except Exception as e:
            print(f"❌ Test failed: {e}")
            
            # Take screenshot for debugging
            try:
                page.screenshot(path="/app/output/test-failure.png")
                print("Screenshot saved to output/test-failure.png")
            except:
                pass
            
            # Print page content for debugging
            try:
                content = page.content()
                print(f"Page content length: {len(content)}")
                if len(content) < 5000:  # Only print if not too long
                    print("Page content:")
                    print(content[:2000] + "..." if len(content) > 2000 else content)
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
