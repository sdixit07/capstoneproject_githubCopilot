import { useEffect, useState } from 'react'
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import ProductList from './ProductList'
import CategoryFilter from './CategoryFilter'
import { fetchCategories, fetchProducts } from './api/productsApi'

function App() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [selectedCategory, setSelectedCategory] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [sortOrder, setSortOrder] = useState('price,asc')
  const [page, setPage] = useState(0)
  const [size] = useState(12)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await fetchCategories()
        setCategories(data)
      } catch (err) {
        setError(err.message)
      }
    }

    loadCategories()
  }, [])

  useEffect(() => {
    const loadProducts = async () => {
      setLoading(true)
      setError(null)

      try {
        const data = await fetchProducts({
          page,
          size,
          search: searchTerm,
          categoryId: selectedCategory,
          sort: sortOrder,
        })
        setProducts(data.items ?? [])
        setTotalPages(data.totalPages ?? 0)
      } catch (err) {
        setProducts([])
        setTotalPages(0)
        setError(err.message)
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
  }, [page, searchTerm, selectedCategory, size, sortOrder])

  const resetToFirstPage = () => setPage(0)

  const handleSearchChange = (event) => {
    setSearchTerm(event.target.value)
    resetToFirstPage()
  }

  const handleSortChange = (event) => {
    setSortOrder(event.target.value)
    resetToFirstPage()
  }

  const handleCategorySelect = (categoryId) => {
    setSelectedCategory(categoryId)
    resetToFirstPage()
  }

  const canGoPrevious = page > 0
  const canGoNext = totalPages > 0 && page < totalPages - 1

  return (
    <div className='container'>
      <h1>Product Catalog</h1><br/>

      <div className='row align-items-center mb-4'>
        <div className='col-md-3 col-sm-12 mb-2'>
          <CategoryFilter categories={categories} selectedCategory={selectedCategory} onSelect={handleCategorySelect} />
        </div>

        <div className='col-md-5 col-sm-12 mb-2'>
        <input 
          type='text'
          className='form-control'
          placeholder='Search for products'
          value={searchTerm}
          onChange={handleSearchChange} />
        </div>

        <div className='col-md-4 col-sm-12 mb-2'>
          <select className='form-control' value={sortOrder} onChange={handleSortChange}>
            <option value="price,asc">Sort by Price: Low to High</option>
            <option value="price,desc">Sort by Price: High to Low</option>
          </select>
        </div>
      </div>

      <div>
        {loading && <p>Loading products...</p>}

        {!loading && error && <div className='alert alert-danger'>{error}</div>}

        {!loading && !error && products.length ? (
          <>
            <ProductList products={products} />
            <div className='d-flex justify-content-between align-items-center mt-4'>
              <button className='btn btn-outline-secondary' onClick={() => setPage(page - 1)} disabled={!canGoPrevious}>
                Previous
              </button>
              <span>Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</span>
              <button className='btn btn-outline-secondary' onClick={() => setPage(page + 1)} disabled={!canGoNext}>
                Next
              </button>
            </div>
          </>
        ) : null}

        {!loading && !error && !products.length && <p>No products to display.</p>}
      </div>

    </div>
  )
}

export default App
